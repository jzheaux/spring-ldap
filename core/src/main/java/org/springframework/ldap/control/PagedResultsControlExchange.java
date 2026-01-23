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

import javax.naming.ldap.Control;
import javax.naming.ldap.PagedResultsResponseControl;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

public class PagedResultsControlExchange implements ControlExchange<SpringLdapPagedResultsControl, PagedResultsResponseControl>, ResolvableTypeProvider {
	private final SpringLdapPagedResultsControl request;
	private final @Nullable PagedResultsResponseControl response;

	public PagedResultsControlExchange(SpringLdapPagedResultsControl request) {
		this.request = request;
		this.response = null;
	}

	PagedResultsControlExchange(SpringLdapPagedResultsControl request, PagedResultsResponseControl response) {
		this.request = request;
		this.response = response;
	}

	public static PagedResultsControlExchange withPageSize(int pageSize) {
		return new PagedResultsControlExchange(new SpringLdapPagedResultsControl(pageSize));
	}

	@Override
	public SpringLdapPagedResultsControl getRequest() {
		return this.request;
	}

	@Override
	public @Nullable PagedResultsResponseControl getResponse() {
		return this.response;
	}

	public boolean hasMore() {
		return this.request.getCookie() != null;
	}

	@Override
	public PagedResultsControlExchange withResponse(Control response) {
		if (!(response instanceof PagedResultsResponseControl paged)) {
			return this;
		}
		SpringLdapPagedResultsControl updated = new SpringLdapPagedResultsControl(
			this.request.getPageSize(), paged.getCookie(), this.request.isCritical());
		return new PagedResultsControlExchange(updated, paged);
	}

	@Override
	public @Nullable ResolvableType getResolvableType() {
		return ResolvableType.forClass(PagedResultsResponseControl.class);
	}
}
