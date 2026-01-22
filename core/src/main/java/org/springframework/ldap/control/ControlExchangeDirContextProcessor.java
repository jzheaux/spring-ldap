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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public final class ControlExchangeDirContextProcessor<S extends Control, T extends Control> implements DirContextProcessor {

	private final Log log = LogFactory.getLog(getClass());

	private ControlExchange<S, T> exchange;

	private final Consumer<DirContext> noResultResponseControlHandler = (ctx) -> {
		this.log.debug("Failed to find response control");
	};

	/**
	 * Construct this {@link DirContextProcessor}, providing the {@link ControlExchange}
	 * to use
	 * @param exchange {@link ControlExchange} to use
	 */
	public ControlExchangeDirContextProcessor(ControlExchange<S, T> exchange) {
		this.exchange = exchange;
	}

	@Override
	public void preProcess(DirContext ctx) throws NamingException {
		if (!(ctx instanceof LdapContext ldap)) {
			throw new IllegalArgumentException("ctx must be of type LdapContext");
		}
		Control[] controls = ldap.getRequestControls();
		if (controls == null) {
			ldap.setRequestControls(new Control[] { this.exchange.getRequest() });
			return;
		}
		List<Control> updated = new ArrayList<>();
		for (Control control : controls) {
			if (!(control instanceof PagedResultsControl)) {
				updated.add(control);
			} else {
				if (this.log.isTraceEnabled()) {
					this.log.trace("Replacing pre-existing paged results control with "
							+ this.exchange.getRequest());
				}
			}
		}
		updated.add(this.exchange.getRequest());
		ldap.setRequestControls(updated.toArray(Control[]::new));
	}

	@Override
	@SuppressWarnings("unchecked")
	public void postProcess(DirContext ctx) throws NamingException {
		if (!(ctx instanceof LdapContext ldap)) {
			throw new IllegalArgumentException("ctx must be of type LdapContext");
		}
		Control[] responseControls = ldap.getResponseControls();
		if (responseControls == null || responseControls.length == 0) {
			this.noResultResponseControlHandler.accept(ctx);
			return;
		}
		for (Control responseControl : responseControls) {
			ControlExchange<S, T> exchange = this.exchange.withResponse((T) responseControl);
			if (exchange != this.exchange) {
				return;
			}
		}
		this.noResultResponseControlHandler.accept(ctx);
	}

	public ControlExchange<S, T> getExchange() {
		return this.exchange;
	}

}
