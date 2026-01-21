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
import java.util.List;
import java.util.function.Consumer;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.ldap.core.DirContextProcessor;

/**
 * A {@link DirContextProcessor} implementation for managing the paged results control.
 * <p>Paging requires that the same LDAP connection be used across each page; as such, it is
 * your responsibility to reuse the LDAP connection.
 * <p>Spring LDAP's
 * {@link org.springframework.ldap.core.support.SingleContextSource} and also its
 * {@link org.springframework.ldap.transaction.compensating.manager.TransactionAwareContextSourceProxy}
 * will provide this for you if you wire them into your {@link org.springframework.ldap.core.LdapTemplate}
 * and {@link org.springframework.ldap.core.LdapClient} instances.
 *
 * @author Josh Cummings
 * @since 4.1
 * @see org.springframework.ldap.core.support.SingleContextSource
 * @see org.springframework.ldap.transaction.compensating.manager.ContextSourceTransactionManager
 * @see org.springframework.ldap.transaction.compensating.manager.TransactionAwareContextSourceProxy
 */
public final class PagedResultsControlDirContextProcessor implements DirContextProcessor {

	private final Log log = LogFactory.getLog(getClass());

	Request request;

	private @Nullable Response response;

	private final Consumer<DirContext> noPagedResultsResponseHandler = (ctx) -> {
		this.log.debug("Failed to find PagedResultsResponseControl in response");
	};

	/**
	 * Construct this {@link DirContextProcessor}, providing the initial
	 * paged results control {@link Request} to use
	 * @param request the initial paged results control to use
	 */
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
		List<Control> updated = new ArrayList<>();
		for (Control control : controls) {
			if (!(control instanceof PagedResultsControl)) {
				updated.add(control);
			} else {
				if (this.log.isTraceEnabled()) {
					this.log.trace("Replacing pre-existing paged results control with "
							+ this.request);
				}
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
		if (responseControls == null || responseControls.length == 0) {
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

	/**
	 * The current request control to be sent as part of an LDAP request.
	 */
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

		@Override
		public String toString() {
			return "PagedResultsRequest [pageSize=" + this.pageSize +
					", cookie=" + (this.cookie != null) +
					", critical=" + this.delegate.isCritical() + "]";
		}
	}

	/**
	 * The most recent response control retrieved from of an LDAP response.
	 */
	public static final class Response {

		private final int resultSize;
		private final byte @Nullable [] cookie;

		public Response(PagedResultsResponseControl response) {
			this.resultSize = response.getResultSize();
			this.cookie = response.getCookie();
		}

		public int getResultSize() {
			return this.resultSize;
		}

		public byte @Nullable [] getCookie() {
			return this.cookie;
		}

		public boolean hasMore() {
			return this.cookie != null;
		}

	}

}
