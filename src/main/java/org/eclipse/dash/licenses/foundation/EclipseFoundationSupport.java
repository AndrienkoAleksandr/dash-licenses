/*************************************************************************
 * Copyright (c) 2019, The Eclipse Foundation and others.
 * 
 * This program and the accompanying materials are made available under 
 * the terms of the Eclipse Public License 2.0 which accompanies this 
 * distribution, and is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *************************************************************************/
package org.eclipse.dash.licenses.foundation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.util.Timeout;
import org.eclipse.dash.licenses.IContentData;
import org.eclipse.dash.licenses.IContentId;
import org.eclipse.dash.licenses.ILicenseDataProvider;
import org.eclipse.dash.licenses.ISettings;

import com.google.common.flogger.FluentLogger;

public class EclipseFoundationSupport implements ILicenseDataProvider {
	private static final FluentLogger log = FluentLogger.forEnclosingClass();

	private ISettings settings;

	public EclipseFoundationSupport(ISettings settings) {
		this.settings = settings;
	}

	@Override
	public void queryLicenseData(Collection<IContentId> ids, Consumer<IContentData> consumer) {
		if (ids.size() == 0)
			return;

		log.atInfo().log("Querying Eclipse Foundation for license data for %1$d items.", ids.size());

		String url = settings.getLicenseCheckUrl();

		try (CloseableHttpClient httpclient = getHttpClient()) {
			JsonArrayBuilder builder = Json.createBuilderFactory(null).createArrayBuilder();
			ids.stream().forEach(id -> builder.add(id.toString()));
			String json = builder.build().toString();

			HttpPost post = new HttpPost(url);
			List<NameValuePair> parameters = new ArrayList<>();
			parameters.add(new BasicNameValuePair("json", json));

			post.setEntity(new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8));

			try (CloseableHttpResponse response = httpclient.execute(post)) {
				if (response.getCode() == 200) {
					// FIXME Seems like overkill.
					AtomicInteger counter = new AtomicInteger();

					try (InputStream content = response.getEntity().getContent()) {
						JsonReader reader = Json.createReader(new InputStreamReader(content, StandardCharsets.UTF_8));
						JsonObject read = (JsonObject) reader.read();

						JsonObject approved = read.getJsonObject("approved");
						if (approved != null)
							approved.forEach((key, each) -> {
								consumer.accept(new FoundationData(each.asJsonObject()));
								counter.incrementAndGet();
							});

						JsonObject restricted = read.getJsonObject("restricted");
						if (restricted != null)
							restricted.forEach((key, each) -> {
								consumer.accept(new FoundationData(each.asJsonObject()));
								counter.incrementAndGet();
							});

					}
					log.atInfo().log("Found %1$d items.", counter.get());
				} else {
					log.atSevere().log("Eclipse Foundation data search time out; maybe decrease batch size.");
				}
			}
		} catch (IOException e) {
			// FIXME Handle gracefully
			throw new RuntimeException(e);
		}
	}

	private CloseableHttpClient getHttpClient() {
		Timeout timeout = Timeout.of(settings.getTimeout(), TimeUnit.SECONDS);

		// @formatter:off
		RequestConfig config = RequestConfig.custom()
			.setConnectTimeout(timeout)
			.setConnectionRequestTimeout(timeout)
			.build();
		
		// @formatter:on
		SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(timeout).build();
		BasicHttpClientConnectionManager connManager = new BasicHttpClientConnectionManager();
		connManager.setSocketConfig(socketConfig);

		return HttpClientBuilder.create().setDefaultRequestConfig(config).setConnectionManager(connManager).build();
	}
}
