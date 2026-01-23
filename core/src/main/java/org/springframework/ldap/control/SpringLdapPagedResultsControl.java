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

import javax.naming.ldap.Control;
import javax.naming.ldap.PagedResultsControl;

import org.jspecify.annotations.Nullable;

public class SpringLdapPagedResultsControl implements Control {

	final PagedResultsControl delegate;

	private final int pageSize;

	private final byte @Nullable [] cookie;

	public SpringLdapPagedResultsControl(int pageSize) {
		this(pageSize, null, true);
	}

	public SpringLdapPagedResultsControl(int pageSize, byte @Nullable [] cookie, boolean criticality) {
		try {
			this.delegate = new PagedResultsControl(pageSize, cookie, criticality);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException(ex);
		}
		this.pageSize = pageSize;
		this.cookie = cookie;
	}

	@Override
	public String getID() {
		return this.delegate.getID();
	}

	@Override
	public byte[] getEncodedValue() {
		return this.delegate.getEncodedValue();
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
		return "PagedResultsRequest [pageSize=" + this.pageSize + ", cookie=" + (this.cookie != null) + ", critical="
				+ this.delegate.isCritical() + "]";
	}

}
