/*
 * Copyright 2002-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ldap.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ldap.core.DirContextProcessor;

/**
 * @since 4.1
 */
public final class PagedResultsControlDirContextProcessor implements DirContextProcessor {

	private Logger log = LoggerFactory.getLogger(PagedResultsControlDirContextProcessor.class);

	Request request;

	private @Nullable Response response;

	private Consumer<DirContext> noPagedResultsResponseHandler = (ctx) -> {
		this.log.debug("Failed to find PagedResultsResponseControl in response");
	};

	public PagedResultsControlDirContextProcessor(Request request) {
		this.request = request;
	}

	@Override
	public void preProcess(DirContext ctx) throws NamingException {
		if (!(ctx instanceof LdapContext ldap)) {
			throw new IllegalArgumentException("ctx must be of type LdapContext");
		}
		Control[] controls = ldap.getRequestControls();
		if (controls == null) {
			ldap.setRequestControls(new Control[] { this.request.delegate });
			return;
		}
		List<Control> updated = new ArrayList<>(Arrays.asList(controls));
		for (Control control : controls) {
			if (!(control instanceof PagedResultsControl)) {
				updated.add(control);
			}
		}
		updated.add(this.request.delegate);
		ldap.setRequestControls(updated.toArray(Control[]::new));
	}

	@Override
	public void postProcess(DirContext ctx) throws NamingException {
		if (!(ctx instanceof LdapContext ldap)) {
			throw new IllegalArgumentException("ctx must be of type LdapContext");
		}
		Control[] responseControls = ldap.getResponseControls();
		if (responseControls.length == 0) {
			this.noPagedResultsResponseHandler.accept(ctx);
			return;
		}
		for (Control responseControl : responseControls) {
			if (responseControl instanceof PagedResultsResponseControl results) {
				this.request = new Request(this.request, results);
				this.response = new Response(results);
				return;
			}
		}
		this.noPagedResultsResponseHandler.accept(ctx);
	}

	public @Nullable Response getResponse() {
		return this.response;
	}

	public static final class Request {

		final javax.naming.ldap.PagedResultsControl delegate;

		private final int pageSize;

		private final byte @Nullable [] cookie;

		public Request(int pageSize) {
			this(pageSize, null, true);
		}

		public Request(int pageSize, byte @Nullable [] cookie, boolean criticality) {
			try {
				this.delegate = new PagedResultsControl(pageSize, cookie, criticality);
			}
			catch (IOException ex) {
				throw new IllegalArgumentException(ex);
			}
			this.pageSize = pageSize;
			this.cookie = cookie;
		}

		Request(Request request, PagedResultsResponseControl response) {
			this(request.getPageSize(), response.getCookie(), request.isCritical());
		}

		public int getPageSize() {
			return this.pageSize;
		}

		public byte @Nullable [] getCookie() {
			return this.cookie;
		}

		public boolean isCritical() {
			return this.delegate.isCritical();
		}

	}

	public static final class Response {

		private final PagedResultsResponseControl response;

		public Response(PagedResultsResponseControl response) {
			this.response = response;
		}

		public int getResultSize() {
			return this.response.getResultSize();
		}

		public byte @Nullable [] getCookie() {
			return this.response.getCookie();
		}

		public boolean hasMore() {
			return this.getCookie() != null;
		}

	}

}
