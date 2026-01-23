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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.ldap.UncategorizedLdapException;

@Deprecated
final class ControlUtils {

	static final Log log = LogFactory.getLog(ControlUtils.class.getName());

	static Class<?> forControlName(String className, String fallbackClassName) {
		try {
			return Class.forName(className);
		}
		catch (ClassNotFoundException ex) {
			log.debug("Default control classes not found - falling back to LdapBP classes", ex);

			try {
				return Class.forName(fallbackClassName);
			}
			catch (ClassNotFoundException e1) {
				throw new UncategorizedLdapException(
						"Neither default nor fallback class is available - unable to proceed", ex);
			}
		}
	}

}
