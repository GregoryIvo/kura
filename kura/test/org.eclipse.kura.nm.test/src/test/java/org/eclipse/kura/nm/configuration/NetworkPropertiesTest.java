/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.nm.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.freedesktop.dbus.errors.NotSupported;
import org.junit.Test;

public class NetworkPropertiesTest {

	NetworkProperties netProps;
	Map<String, Object> properties = new HashMap<String, Object>();
	Optional<?> optResult;
	String stringResult;
	List<String> stringListResult;
	Map<String, Object> resultMap;

	Boolean hasNullPointExceptionBeenThrown = false;
	Boolean hasNoSuchElementExceptionBeenThrown = false;

	@Test
	public void shouldFailWhenNull() {
		givenNetworkPropertiesBuiltWith(null);
		givenNetworkPropsIsCreated();
		thenANullPointerExceptionOccured();
	}
	
	@Test
	public void shouldFailWhenWmpty() {
		givenNetworkPropertiesBuiltWith(new HashMap<String,Object>());
		givenNetworkPropsIsCreated();
		whenGetPropertiesIsCalled();
		thenResultEquals(new HashMap<String,Object>());
		thenNoExceptionsOccured();
	}

	@Test
	public void shouldReturnPropertiesWithGetProperties() {
		givenMapWith("testKey1", "testString1");
		givenNetworkPropsIsCreated();
		whenGetPropertiesIsCalled();
		thenResultEquals(Collections.singletonMap("testKey1", "testString1"));
		thenNoExceptionsOccured();
	}

	@Test
	public void shouldReturnStringWithGet() {
		givenMapWith("testKey1", "testString1");
		givenNetworkPropsIsCreated();
		whenGetIsCalledWith("testKey1", String.class);
		thenResultEquals("testString1");
		thenNoExceptionsOccured();
	}
	
	@Test
	public void shouldReturnStringWithGetNull() {
		givenMapWith("testKey1", null);
		givenNetworkPropsIsCreated();
		whenGetIsCalledWith("testKey1", String.class);
		thenANoSuchElementExceptionOccured();
	}
	
	@Test
	public void shouldReturnStringWithGetMissing() {
		givenMapWith("testKey1", null);
		givenNetworkPropsIsCreated();
		whenGetIsCalledWith("testKey1-nonExistant", String.class);
		thenANoSuchElementExceptionOccured();
	}
	
	@Test
	public void shouldReturnStringWithGetEmptyString() {
		givenMapWith("", "");
		givenNetworkPropsIsCreated();
		whenGetIsCalledWith("", String.class);
		thenResultEquals("");
		thenNoExceptionsOccured();
	}

	@Test
	public void shouldReturnStringWithGetOpt() {
		givenMapWith("testKey1", "testString1");
		givenNetworkPropsIsCreated();
		whenGetOptIsCalledWith("testKey1", String.class);
		thenResultEquals(Optional.of("testString1"));
		thenNoExceptionsOccured();
	}

	@Test
	public void shouldReturnEmptyGetOptEmpty() {
		givenMapWith("testKeyEmpty", "");
		givenNetworkPropsIsCreated();
		whenGetOptIsCalledWith("testKeyEmpty", String.class);
		thenResultEquals(Optional.empty());
		thenNoExceptionsOccured();
	}

	@Test
	public void shouldReturnEmptyGetOptNull() {
		givenMapWith("testKeyNull", null);
		givenNetworkPropsIsCreated();
		whenGetOptIsCalledWith("testKeyNull", String.class);
		thenResultEquals(Optional.empty());
		thenNoExceptionsOccured();
	}

	@Test
	public void shouldReturnEmptyGetOptEmptyKey() {
		givenMapWith("testKeyNull", null);
		givenMapWith("testKey1", "testString1");
		givenMapWith("testKeyNull2", null);
		givenNetworkPropsIsCreated();
		whenGetOptIsCalledWith("", String.class);
		thenResultEquals(Optional.empty());
		thenNoExceptionsOccured();
	}

	@Test
	public void shouldReturnStringListGetStringListWithCommaSeperatedString() {
		givenMapWith("testKeyNull", null);
		givenMapWith("testKey1", "testString1");
		givenMapWith("testKey-comma-seperated", "commaSeperated1,commaSeperated2,commaSeperated3");
		givenNetworkPropsIsCreated();
		whenGetStringListIsCalledWith("testKey-comma-seperated");
		thenResultEquals(Arrays.asList("commaSeperated1", "commaSeperated2", "commaSeperated3"));
		thenNoExceptionsOccured();
	}

	@Test
	public void shouldReturnStringListGetStringListWithCommaSeperatedStringMalformed() {
		givenMapWith("testKeyNull", null);
		givenMapWith("testKey1", "testString1");
		givenMapWith("testKey-comma-seperated",
				",,   ,,,commaSeperated1, ,,,,commaSeperated2,   ,,commaSeperated3,");
		givenNetworkPropsIsCreated();
		whenGetStringListIsCalledWith("testKey-comma-seperated");
		thenResultEquals(Arrays.asList("commaSeperated1", "commaSeperated2", "commaSeperated3"));
		thenNoExceptionsOccured();
	}
	
	@Test
	public void shouldReturnStringListGetStringListWithCommaSeperatedStringNull() {
		givenMapWith("testKey-comma-seperated", null);
		givenNetworkPropsIsCreated();
		whenGetStringListIsCalledWith("testKey-comma-seperated");
		thenANoSuchElementExceptionOccured();
	}
	
	@Test
	public void shouldReturnStringListGetStringListWithCommaSeperatedStringMissing() {
		givenMapWith("testKey-comma-seperated", null);
		givenNetworkPropsIsCreated();
		whenGetStringListIsCalledWith("testKey-comma-seperated-not-existant");
		thenANoSuchElementExceptionOccured();
	}

	@Test
	public void shouldReturnStringListGetStringListWithRegularString() {
		givenMapWith("testKey1", "testString1");
		givenNetworkPropsIsCreated();
		whenGetStringListIsCalledWith("testKey1");
		thenResultEquals(Arrays.asList("testString1"));
		thenNoExceptionsOccured();
	}

	@Test
	public void shouldReturnStringListGetStringListWithNull() {
		givenMapWith("testKeyNull", null);
		givenNetworkPropsIsCreated();
		whenGetStringListIsCalledWith("testKeyNull");
		thenResultEquals(Arrays.asList());
		thenNoExceptionsOccured();
	}

	@Test
	public void shouldReturnOptionalStringListGetStringListOptWithCommaSeperatedString() {
		givenMapWith("testKey-comma-seperated", "commaSeperated1,commaSeperated2,commaSeperated3");
		givenNetworkPropsIsCreated();
		whenGetStringListOptIsCalledWith("testKey-comma-seperated");
		thenResultEquals(Optional.of(Arrays.asList("commaSeperated1", "commaSeperated2", "commaSeperated3")));
		thenNoExceptionsOccured();
	}

	@Test
	public void shouldReturnOptionalStringListGetStringListOptWithCommaSeperatedStringMalformed() {
		givenMapWith("testKey-comma-seperated",
				", , ,,,,commaSeperated1, , , ,,,,,commaSeperated2,,,, ,, ,,commaSeperated3,, , ,,,, ,");
		givenNetworkPropsIsCreated();
		whenGetStringListOptIsCalledWith("testKey-comma-seperated");
		thenResultEquals(Optional.of(Arrays.asList("commaSeperated1", "commaSeperated2", "commaSeperated3")));
		thenNoExceptionsOccured();
	}

	@Test
	public void shouldReturnOptionalStringListGetStringListOptWithString() {
		givenMapWith("testKey1", "testString1");
		givenNetworkPropsIsCreated();
		whenGetStringListOptIsCalledWith("testKey1");
		thenResultEquals(Optional.of(Arrays.asList("testString1")));
		thenNoExceptionsOccured();
	}

	@Test
	public void shouldReturnEmptyGetStringListOptWithNull() {
		givenMapWith("testKeyNull", null);
		givenNetworkPropsIsCreated();
		whenGetStringListOptIsCalledWith("testKeyNull");
		thenResultEquals(Optional.empty());
		thenNoExceptionsOccured();
	}

	@Test
	public void shouldReturnEmptyGetStringListOptWithNoKey() {
		givenMapWith("testKeyNull", null);
		givenNetworkPropsIsCreated();
		whenGetStringListOptIsCalledWith("");
		thenResultEquals(Optional.empty());
		thenNoExceptionsOccured();
	}

	/*
	 * Given
	 */

	public void givenNetworkPropertiesBuiltWith(Map<String, Object> properties) {
		this.properties = properties;
	}

	public void givenMapWith(String key, Object pair) {
		properties.put(key, pair);
	}

	public void givenNetworkPropsIsCreated() {

		try {
			netProps = new NetworkProperties(this.properties);
			this.hasNullPointExceptionBeenThrown = false;
			this.hasNoSuchElementExceptionBeenThrown = false;
		} catch (NullPointerException e) {
			this.hasNullPointExceptionBeenThrown = true;
		} catch (NoSuchElementException e) {
			this.hasNoSuchElementExceptionBeenThrown = true;			
		}
	} 

	/*
	 * When
	 */

	public void whenGetPropertiesIsCalled() {
		this.resultMap = this.netProps.getProperties();
	}

	public void whenGetIsCalledWith(String key, Object clazz) {

		if (clazz == String.class) {
			this.stringResult = this.netProps.get(String.class, key, "");
		} else {
			throw new NotSupported("Data type is not supported with this Test");
		}
	}

	public void whenGetOptIsCalledWith(String key, Object clazz) {
		if (clazz == String.class) {
			this.optResult = this.netProps.getOpt(String.class, key, "");
		} else {
			throw new NotSupported("Data type is not supported with this Test");
		}
	}

	public void whenGetStringListIsCalledWith(String key) {
		this.stringListResult = this.netProps.getStringList(key, "");
	}

	public void whenGetStringListOptIsCalledWith(String key) {
		this.optResult = this.netProps.getOptStringList(key, "");
	}

	/*
	 * Then
	 */

	public void thenResultEquals(String result) {
		assertEquals(result, this.stringResult);
	}

	public void thenResultEquals(Map<String, Object> result) {
		assertEquals(this.resultMap, result);
	}

	public void thenResultEquals(Optional<?> result) {
		assertEquals(this.optResult, result);
	}

	public void thenResultEquals(List<String> result) {
		assertEquals(this.stringListResult, result);
	}

	public void thenANullPointerExceptionOccured() {
		assertTrue(this.hasNullPointExceptionBeenThrown);
	}
	public void thenANoSuchElementExceptionOccured() {
		assertTrue(this.hasNoSuchElementExceptionBeenThrown);
	}
	
	public void thenNoExceptionsOccured() {
		assertFalse(this.hasNullPointExceptionBeenThrown);
		assertFalse(this.hasNoSuchElementExceptionBeenThrown);
	}

}
