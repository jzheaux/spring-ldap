/*
 * Copyright 2006-present the original author or authors.
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
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;

import org.jspecify.annotations.Nullable;

import org.springframework.ldap.UncategorizedLdapException;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * DirContextProcessor implementation for managing the paged results control. Note that
 * due to the internal workings of <code>LdapTemplate</code>, the target connection is
 * closed after each LDAP call. The PagedResults control require the same connection be
 * used for each call, which means we need to make sure the target connection is never
 * actually closed. There's basically two ways of making this happen: use the
 * <code>SingleContextSource</code> implementation or make sure all calls happen within a
 * single LDAP transaction (using <code>ContextSourceTransactionManager</code>).
 *
 * @author Mattias Hellborg Arthursson
 * @author Ulrik Sandberg
 * @deprecated Use PagedResultsDirContextProcessor instead.
 */
public class PagedResultsRequestControl extends AbstractRequestControlDirContextProcessor {

	private static final boolean CRITICAL_CONTROL = true;

	private static final String DEFAULT_REQUEST_CONTROL = "javax.naming.ldap.PagedResultsControl";

	private static final String LDAPBP_REQUEST_CONTROL = "com.sun.jndi.ldap.ctl.PagedResultsControl";

	private static final String DEFAULT_RESPONSE_CONTROL = "javax.naming.ldap.PagedResultsResponseControl";

	private static final String LDAPBP_RESPONSE_CONTROL = "com.sun.jndi.ldap.ctl.PagedResultsResponseControl";

	private int pageSize;

	private @Nullable PagedResultsCookie cookie;

	private int resultSize;

	private boolean critical = CRITICAL_CONTROL;

	private Class responseControlClass;

	private Class requestControlClass;

	/**
	 * Constructs a new instance. This constructor should be used when performing the
	 * first paged search operation, when no other results have been retrieved.
	 * @param pageSize the page size.
	 */
	public PagedResultsRequestControl(int pageSize) {
		this(pageSize, null);
	}

	/**
	 * Constructs a new instance with the supplied page size and cookie. The cookie must
	 * be the exact same instance as received from a previous paged resullts search, or
	 * <code>null</code> if it is the first in an operation sequence.
	 * @param pageSize the page size.
	 * @param cookie the cookie, as received from a previous search.
	 */
	public PagedResultsRequestControl(int pageSize, @Nullable PagedResultsCookie cookie) {
		this.pageSize = pageSize;
		this.cookie = cookie;

		loadControlClasses();
	}

	private void loadControlClasses() {
		try {
			this.requestControlClass = Class.forName(DEFAULT_REQUEST_CONTROL);
			this.responseControlClass = Class.forName(DEFAULT_RESPONSE_CONTROL);
		}
		catch (ClassNotFoundException ex) {
			this.log.debug("Default control classes not found - falling back to LdapBP classes", ex);

			try {
				this.requestControlClass = Class.forName(LDAPBP_REQUEST_CONTROL);
				this.responseControlClass = Class.forName(LDAPBP_RESPONSE_CONTROL);
			}
			catch (ClassNotFoundException e1) {
				throw new UncategorizedLdapException(
						"Neither default nor fallback classes are available - unable to proceed", ex);
			}

		}
	}

	/**
	 * Get the cookie.
	 * @return the cookie.
	 */
	public @Nullable PagedResultsCookie getCookie() {
		return this.cookie;
	}

	/**
	 * Get the page size.
	 * @return the page size.
	 */
	public int getPageSize() {
		return this.pageSize;
	}

	/**
	 * Get the total estimated number of entries that matches the issued search. Note that
	 * this value is optional for the LDAP server to return, so it does not always contain
	 * any valid data.
	 * @return the estimated result size, if returned from the server.
	 */
	public int getResultSize() {
		return this.resultSize;
	}

	/**
	 * Set the class of the expected ResponseControl for the paged results response.
	 * @param responseControlClass Class of the expected response control.
	 */
	public void setResponseControlClass(Class responseControlClass) {
		this.responseControlClass = responseControlClass;
	}

	public void setRequestControlClass(Class requestControlClass) {
		this.requestControlClass = requestControlClass;
	}

	/*
	 * @see org.springframework.ldap.control.AbstractRequestControlDirContextProcessor
	 * #createRequestControl()
	 */

	public Control createRequestControl() {
		byte[] actualCookie = null;
		if (this.cookie != null) {
			actualCookie = this.cookie.getCookie();
		}
		try {
			return requestControl(this.pageSize, actualCookie, this.critical);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/*
	 * @see org.springframework.ldap.core.DirContextProcessor#postProcess(javax.naming
	 * .directory.DirContext)
	 */

	public void postProcess(DirContext ctx) throws NamingException {

		LdapContext ldapContext = (LdapContext) ctx;
		Control[] responseControls = ldapContext.getResponseControls();
		if (responseControls == null) {
			responseControls = new Control[0];
		}

		// Go through response controls and get info, regardless of class
		for (int i = 0; i < responseControls.length; i++) {
			Control responseControl = responseControls[i];

			// check for match, try fallback otherwise
			if (responseControl.getClass().isAssignableFrom(this.responseControlClass)) {
				PagedResultsResponseControlInterface control = responseControl(responseControl);
				this.cookie = new PagedResultsCookie(control.getCookie());
				this.resultSize = control.getResultSize();
				return;
			}
		}

		this.log.error("No matching response control found for paged results - looking for '{}",
				this.responseControlClass);
	}

	private Control requestControl(int pageSize, byte @Nullable [] cookie, boolean critical) throws IOException {
		if (DEFAULT_REQUEST_CONTROL.equals(this.requestControlClass.getName())) {
			return new javax.naming.ldap.PagedResultsControl(pageSize, cookie, critical);
		}
		if (LDAPBP_REQUEST_CONTROL.equals(this.requestControlClass.getName())) {
			return new com.sun.jndi.ldap.ctl.PagedResultsControl(pageSize, cookie, critical);
		}
		Constructor<?> constructor = ClassUtils.getConstructorIfAvailable(this.requestControlClass,
			int.class, byte[].class, boolean.class);
		if (constructor == null) {
			throw new IllegalArgumentException("Failed to find an appropriate RequestControl constructor");
		}
		try {
			return (Control) constructor.newInstance(this.pageSize, cookie, this.critical);
		}
		catch (Exception ex) {
			ReflectionUtils.handleReflectionException(ex);
			throw new UndeclaredThrowableException(ex);
		}
	}

	private PagedResultsResponseControlInterface responseControl(Control control) {
		if (DEFAULT_RESPONSE_CONTROL.equals(control.getClass().getName())) {
			return new JavaxPagedResultsResponseControl(control);
		}
		if (LDAPBP_RESPONSE_CONTROL.equals(control.getClass().getName())) {
			return new SunPagedResultsResponseControl(control);
		}
		return (PagedResultsResponseControlInterface) Proxy.newProxyInstance(getClass().getClassLoader(),
			new Class[] { this.responseControlClass }, (p, m, a) -> m.invoke(p, a));
	}

	interface PagedResultsResponseControlInterface {
		int getResultSize();

		byte @Nullable [] getCookie();
	}

	static final class SunPagedResultsResponseControl implements PagedResultsResponseControlInterface {
		private final com.sun.jndi.ldap.ctl.PagedResultsResponseControl control;

		SunPagedResultsResponseControl(Control control) {
			this.control = (com.sun.jndi.ldap.ctl.PagedResultsResponseControl) control;
		}

		@Override
		public int getResultSize() {
			return this.control.getResultSize();
		}

		@Override
		public byte @Nullable [] getCookie() {
			return this.control.getCookie();
		}
	}

	static final class JavaxPagedResultsResponseControl implements PagedResultsResponseControlInterface {
		private final javax.naming.ldap.PagedResultsResponseControl control;

		public JavaxPagedResultsResponseControl(Control control) {
			this.control = (javax.naming.ldap.PagedResultsResponseControl) control;
		}

		@Override
		public int getResultSize() {
			return this.control.getResultSize();
		}

		@Override
		public byte @Nullable [] getCookie() {
			return this.control.getCookie();
		}
	}

}
