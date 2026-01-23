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
import java.util.Arrays;

import javax.naming.ldap.Control;
import javax.naming.ldap.SortControl;

public class SpringLdapSortControl implements Control {

	final SortControl delegate;

	private final String[] sortBy;

	public SpringLdapSortControl(String sortBy) {
		this(new String[] { sortBy }, true);
	}

	public SpringLdapSortControl(String[] sortBy, boolean criticality) {
		try {
			this.delegate = new SortControl(sortBy, criticality);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException(ex);
		}
		this.sortBy = sortBy;
	}

	@Override
	public String getID() {
		return this.delegate.getID();
	}

	@Override
	public byte[] getEncodedValue() {
		return this.delegate.getEncodedValue();
	}

	public String[] getSortBy() {
		return sortBy;
	}

	public boolean isCritical() {
		return this.delegate.isCritical();
	}

	@Override
	public String toString() {
		return "PagedResultsRequest [sortBy=" + Arrays.toString(this.sortBy) + ", critical="
				+ this.delegate.isCritical() + "]";
	}

}
