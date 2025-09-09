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

package org.springframework.ldap.odm.tools;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;

import org.springframework.util.StringUtils;

/**
 * Simple value class to hold the schema of an attribute.
 * <p>
 * It is only public to allow Freemarker access.
 *
 * @author Paul Harvey &lt;paul.at.pauls-place.me.uk&gt;
 */
@NullMarked
public final class AttributeSchema {

	private final String name;

	private final String syntax;

	private final boolean isMultiValued;

	private final boolean isPrimitive;

	private final String scalarType;

	private final boolean isBinary;

	private final boolean isArray;

	public AttributeSchema(String name, String syntax, boolean isMultiValued, boolean isPrimitive, boolean isBinary,
			boolean isArray, String scalarType) {
		this.name = name;
		this.syntax = syntax;
		this.isMultiValued = isMultiValued;
		this.isPrimitive = isPrimitive;
		this.scalarType = scalarType;
		this.isBinary = isBinary;
		this.isArray = isArray;
	}

	public boolean getIsArray() {
		return this.isArray;
	}

	public boolean getIsBinary() {
		return this.isBinary;
	}

	public boolean getIsPrimitive() {
		return this.isPrimitive;
	}

	public String getScalarType() {
		return this.scalarType;
	}

	public String getName() {
		return this.name;
	}

	public String getJavaName() {
		return StringUtils.replace(this.name, "-", "");
	}

	public String getSyntax() {
		return this.syntax;
	}

	public boolean getIsMultiValued() {
		return this.isMultiValued;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof AttributeSchema that)) {
			return false;
		}
		return Objects.equals(this.isArray, that.isArray) && Objects.equals(this.isBinary, that.isBinary)
				&& Objects.equals(this.isMultiValued, that.isMultiValued)
				&& Objects.equals(this.isPrimitive, that.isPrimitive) && Objects.equals(this.name, that.name)
				&& Objects.equals(this.scalarType, that.scalarType) && Objects.equals(this.syntax, that.syntax);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.isArray, this.isBinary, this.isMultiValued, this.isPrimitive, this.name,
				this.scalarType, this.syntax);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return String.format(
				"{ name=%1$s, syntax=%2$s, isMultiValued=%3$s, isPrimitive=%4$s, isBinary=%5$s, isArray=%6$s, scalarType=%7$s }",
				this.name, this.syntax, this.isMultiValued, this.isPrimitive, this.isBinary, this.isArray,
				this.scalarType);
	}

}
