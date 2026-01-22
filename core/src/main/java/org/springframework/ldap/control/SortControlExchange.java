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
import javax.naming.ldap.SortResponseControl;

import org.jspecify.annotations.Nullable;

public class SortControlExchange implements ControlExchange<SpringSortControl, SortResponseControl> {
	private final SpringSortControl request;
	private final @Nullable SortResponseControl response;

	public SortControlExchange(SpringSortControl request) {
		this.request = request;
		this.response = null;
	}

	SortControlExchange(SpringSortControl request, SortResponseControl response) {
		this.request = request;
		this.response = response;
	}

	@Override
	public SpringSortControl getRequest() {
		return this.request;
	}

	@Override
	public @Nullable SortResponseControl getResponse() {
		return this.response;
	}

	@Override
	public SortControlExchange withResponse(Control response) {
		if (!(response instanceof SortResponseControl sorted)) {
			return this;
		}
		return new SortControlExchange(this.request, sorted);
	}
}
