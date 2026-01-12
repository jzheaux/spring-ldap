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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Convenient base class useful when implementing a standard DirContextProcessor which has
 * a request control and a response control. It handles the loading of the control
 * classes, using fallback implementations specified by the subclass if necessary. It
 * handles the request control constructor invocation; it only needs the constructor
 * arguments to be provided. It also handles most of the work in the post processing of
 * the response control, only delegating to a template method for the actual value
 * retrieval. In short, it makes it easy to implement a custom DirContextProcessor.
 * <p>
 *
 * <pre>
 * public class SortControlDirContextProcessor extends AbstractFallbackRequestAndResponseControlDirContextProcessor {
 * 	String sortKey;
 *
 * 	private boolean sorted = false;
 *
 * 	private int resultCode = -1;
 *
 * 	public SortControlDirContextProcessor(String sortKey) {
 * 		this.sortKey = sortKey;
 *
 * 		defaultRequestControl = &quot;javax.naming.ldap.SortControl&quot;;
 * 		defaultResponseControl = &quot;com.sun.jndi.ldap.ctl.SortControl&quot;;
 * 		fallbackRequestControl = &quot;javax.naming.ldap.SortResponseControl&quot;;
 * 		fallbackResponseControl = &quot;com.sun.jndi.ldap.ctl.SortResponseControl&quot;;
 *
 * 		loadControlClasses();
 * 	}
 *
 * 	public boolean isSorted() {
 * 		return sorted;
 * 	}
 *
 * 	public int getResultCode() {
 * 		return resultCode;
 * 	}
 *
 * 	public Control createRequestControl() {
 * 		return super.createRequestControl(new Class[] { String[].class, boolean.class }, new Object[] {
 *				new String[] { sortKey }, Boolean.valueOf(critical) });
 * 	}
 *
 * 	protected void handleResponse(Object control) {
 * 		Boolean result = (Boolean) invokeMethod(&quot;isSorted&quot;, responseControlClass, control);
 * 		this.sorted = result.booleanValue();
 * 		Integer code = (Integer) invokeMethod(&quot;getResultCode&quot;, responseControlClass, control);
 * 		resultCode = code.intValue();
 * 	}
 * }
 * </pre>
 *
 * @author Ulrik Sandberg
 */
public abstract class AbstractFallbackRequestAndResponseControlDirContextProcessor
		extends AbstractRequestControlDirContextProcessor {

	private static final boolean CRITICAL_CONTROL = true;

	protected Class<?> responseControlClass = Void.class;

	protected Class<?> requestControlClass = Void.class;

	protected boolean critical = CRITICAL_CONTROL;

	@Deprecated
	protected @Nullable String defaultRequestControl;

	@Deprecated
	protected @Nullable String defaultResponseControl;

	@Deprecated
	protected @Nullable String fallbackRequestControl;

	@Deprecated
	protected @Nullable String fallbackResponseControl;

	@Deprecated
	public AbstractFallbackRequestAndResponseControlDirContextProcessor() {}

	public AbstractFallbackRequestAndResponseControlDirContextProcessor(
		Class<?> requestControlClass, Class<?> responseControlClass) {
		this.requestControlClass = requestControlClass;
		this.responseControlClass = responseControlClass;
	}

	@Deprecated
	protected void loadControlClasses() {
		Assert.notNull(this.defaultRequestControl, "defaultRequestControl must not be null");
		Assert.notNull(this.defaultResponseControl, "defaultResponseControl must not be null");
		Assert.notNull(this.fallbackRequestControl, "fallbackRequestControl must not be null");
		Assert.notNull(this.fallbackResponseControl, "fallbackReponseControl must not be null");
		this.requestControlClass = ControlUtils.forControlName(this.defaultRequestControl, this.fallbackRequestControl);
		this.responseControlClass = ControlUtils.forControlName(this.defaultResponseControl, this.fallbackResponseControl);
	}

	/**
	 * Set the class of the expected ResponseControl for the sorted result response.
	 * @param responseControlClass Class of the expected response control.
	 * @deprecated Please provide the response control class in the constructor
	 */
	@Deprecated
	public void setResponseControlClass(Class<?> responseControlClass) {
		this.responseControlClass = responseControlClass;
	}

	/**
	 * Set the class of the expected RequestControl for the request.
	 * @param requestControlClass Class of the expected request control.
	 * @deprecated Please provide the request control class in the constructor
	 */
	@Deprecated
	public void setRequestControlClass(Class<?> requestControlClass) {
		this.requestControlClass = requestControlClass;
	}

	/**
	 * Utility method for invoking a method on a Control.
	 * @param method name of method to invoke
	 * @param clazz Class of the object that the method should be invoked on
	 * @param control Instance that the method should be invoked on
	 * @return the invocation result, if any
	 */
	protected @Nullable Object invokeMethod(String method, Class<?> clazz, Object control) {
		Method actualMethod = ReflectionUtils.findMethod(clazz, method);
		return ReflectionUtils.invokeMethod(Objects.requireNonNull(actualMethod), control);
	}

	/**
	 * Creates a request control using the constructor parameters given in
	 * <code>params</code>.
	 * @param paramTypes Types of the constructor parameters
	 * @param params Actual constructor parameters
	 * @return Control to be used by the DirContextProcessor
	 */
	public Control createRequestControl(Class<?>[] paramTypes, @Nullable Object[] params) {
		Constructor<?> constructor = ClassUtils.getConstructorIfAvailable(this.requestControlClass, paramTypes);
		if (constructor == null) {
			throw new IllegalArgumentException("Failed to find an appropriate RequestControl constructor");
		}

		Control result = null;
		try {
			result = (Control) constructor.newInstance(params);
		}
		catch (Exception ex) {
			ReflectionUtils.handleReflectionException(ex);
		}

		return result;
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
		for (Control responseControl : responseControls) {
			// check for match, try fallback otherwise
			if (responseControl.getClass().isAssignableFrom(this.responseControlClass)) {
				handleResponse(responseControl);
				return;
			}
		}

		this.log.info("No matching response control found - looking for '" + this.responseControlClass);
	}

	/**
	 * Set whether this control should be indicated as critical.
	 * @param critical whether the control is critical.
	 * @since 2.0
	 */
	public void setCritical(boolean critical) {
		this.critical = critical;
	}

	protected abstract void handleResponse(Object control);

}
