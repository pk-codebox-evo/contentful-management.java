/*
 * Copyright (C) 2017 Contentful GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.contentful.java.cma

import com.contentful.java.cma.lib.ModuleTestUtils
import com.contentful.java.cma.lib.TestCallback
import com.contentful.java.cma.lib.TestUtils
import com.contentful.java.cma.model.CMAEntry
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import java.io.IOException
import kotlin.test.*
import org.junit.Test as test

class EntryTests : BaseTest() {
    @test fun testArchive() {
        val responseBody = TestUtils.fileToString("entry_archive_response.json")
        server!!.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))
        val entry = CMAEntry().setId("entryid").setSpaceId("spaceid")
        assertFalse(entry.isArchived)
        val result = assertTestCallback(
                client!!.entries().async().archive(entry, TestCallback()) as TestCallback)!!
        assertTrue(result.isArchived)

        // Request
        val recordedRequest = server!!.takeRequest()
        assertEquals("PUT", recordedRequest.method)
        assertEquals("/spaces/spaceid/entries/entryid/archived", recordedRequest.path)
    }

    @test fun testCreate() {
        val requestBody = TestUtils.fileToString("entry_create_request.json")
        val responseBody = TestUtils.fileToString("entry_create_response.json")
        server!!.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val entry = CMAEntry()
                .setField("fid1", "value1", "en-US")
                .setField("fid2", "value2", "en-US")

        val result = assertTestCallback(client!!.entries()
                .async()
                .create("spaceid", "ctid", entry, TestCallback()) as TestCallback)!!

        assertEquals(2, result.fields.size)

        val entries = result.fields.entries.toList()
        assertEquals("id1", entries[0].key)
        assertEquals("id2", entries[1].key)

        assertEquals("value1", entries[0].value["en-US"])
        assertEquals("value2", entries[1].value["en-US"])

        // Request
        val recordedRequest = server!!.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/spaces/spaceid/entries", recordedRequest.path)
        assertEquals(requestBody, recordedRequest.utf8Body)
        assertEquals("ctid", recordedRequest.getHeader("X-Contentful-Content-Type"))
    }

    @test fun testCreateWithId() {
        val requestBody = TestUtils.fileToString("entry_create_request.json")
        val responseBody = TestUtils.fileToString("entry_create_response.json")
        server!!.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val entry = CMAEntry()
                .setId("entryid")
                .setField("fid1", "value1", "en-US")
                .setField("fid2", "value2", "en-US")

        assertTestCallback(client!!.entries().async().create(
                "spaceid", "ctid", entry, TestCallback()) as TestCallback)

        // Request
        val recordedRequest = server!!.takeRequest()
        assertEquals("PUT", recordedRequest.method)
        assertEquals("/spaces/spaceid/entries/entryid", recordedRequest.path)
        assertEquals(requestBody, recordedRequest.utf8Body)
        assertEquals("ctid", recordedRequest.getHeader("X-Contentful-Content-Type"))
    }

    @test fun testCreateWithLinks() {
        val responseBody = TestUtils.fileToString("entry_create_links_request.json")
        server!!.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val locale = "en-US"

        val foo = CMAEntry().setId("foo").setField("name", "foo", locale)
        val bar = CMAEntry().setId("bar").setField("name", "bar", locale)

        foo.setField("link", bar, locale)
        foo.setField("link", bar, "he-IL")
        foo.setField("array", listOf(bar), locale)

        bar.setField("link", foo, locale)

        client!!.entries().create("space", "type", foo)

        val requestBody = TestUtils.fileToString("entry_create_links_request.json")
        val request = server!!.takeRequest()
        assertEquals(requestBody, request.utf8Body)
    }

    @test(expected = RuntimeException::class)
    fun testCreateWithBadLinksThrows() {
        val foo = CMAEntry().setId("bar").setField("link", CMAEntry(), "en-US")
        server!!.enqueue(MockResponse().setResponseCode(200))
        try {
            client!!.entries().create("space", "type", foo)
        } catch(e: RuntimeException) {
            assertEquals("Entry contains link to draft resource (has no ID).", e.message)
            throw e
        }
    }

    @test fun testDelete() {
        val requestBody = "203"
        server!!.enqueue(MockResponse().setResponseCode(200).setBody(requestBody))
        assertTestCallback(client!!.entries().async().delete(
                "spaceid", "entryid", TestCallback()) as TestCallback)

        // Request
        val recordedRequest = server!!.takeRequest()
        assertEquals("DELETE", recordedRequest.method)
        assertEquals("/spaces/spaceid/entries/entryid", recordedRequest.path)
    }

    @test fun testFetchAll() {
        val responseBody = TestUtils.fileToString("entry_fetch_all_response.json")
        server!!.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val result = assertTestCallback(client!!.entries().async().fetchAll(
                "spaceid", TestCallback()) as TestCallback)!!

        assertEquals("Array", result.sys["type"])
        assertEquals(1, result.total)
        assertEquals(1, result.items.size)

        val item = result.items[0]
        assertEquals(2, item.fields.size)
        assertNotNull(item.fields["name"])
        assertNotNull(item.fields["type"])
        assertNull(result.includes)

        // Request
        val request = server!!.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/spaces/spaceid/entries?limit=100", request.path)
    }

    @test fun testFetchAllWithQuery() {
        server!!.enqueue(MockResponse().setResponseCode(200).setBody(
                TestUtils.fileToString("entry_fetch_all_response.json")))

        val query = hashMapOf(Pair("skip", "1"), Pair("limit", "2"), Pair("content_type", "foo"))

        assertTestCallback(client!!.entries().async().fetchAll(
                "spaceid", query, TestCallback()) as TestCallback)

        // Request
        val request = server!!.takeRequest()
        val url = HttpUrl.parse(server!!.url(request.path).toString())
        assertEquals("1", url.queryParameter("skip"))
        assertEquals("2", url.queryParameter("limit"))
        assertEquals("foo", url.queryParameter("content_type"))
    }

    @test fun testFetchWithId() {
        val responseBody = TestUtils.fileToString("entry_fetch_one_response.json")
        server!!.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val result = assertTestCallback(client!!.entries().async().fetchOne(
                "space", "entry", TestCallback()) as TestCallback)!!

        val sys = result.sys
        val fields = result.fields

        assertEquals("Entry", sys["type"])
        assertEquals("entryid", sys["id"])
        assertEquals(2, fields.size)
        assertEquals("http://www.url.com", fields["url"]!!["en-US"])
        assertEquals("value", fields["key"]!!["en-US"])

        // Request
        val request = server!!.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/spaces/space/entries/entry", request.path)
    }

    @test fun testParseEntryWithList() {
        gson!!.fromJson(
                TestUtils.fileToString("entry_with_list_object.json"),
                CMAEntry::class.java)
    }

    @test fun testUpdate() {
        val requestBody = TestUtils.fileToString("entry_update_request.json")
        val responseBody = TestUtils.fileToString("entry_update_response.json")
        server!!.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val result = assertTestCallback(client!!.entries().async().update(CMAEntry()
                .setId("entryid")
                .setSpaceId("spaceid")
                .setVersion(1.0)
                .setField("fid1", "newvalue1", "en-US")
                .setField("fid2", "newvalue2", "en-US"), TestCallback()) as TestCallback)!!

        val fields = result.fields.entries.toList()

        assertEquals("entryid", result.sys["id"])
        assertEquals("Entry", result.sys["type"])
        assertEquals(2, fields.size)
        assertEquals("fid1", fields[0].key)
        assertEquals("newvalue1", fields[0].value["en-US"])
        assertEquals("fid2", fields[1].key)
        assertEquals("newvalue2", fields[1].value["en-US"])

        // Request
        val recordedRequest = server!!.takeRequest()
        assertEquals("PUT", recordedRequest.method)
        assertEquals("/spaces/spaceid/entries/entryid", recordedRequest.path)
        assertNotNull(recordedRequest.getHeader("X-Contentful-Version"))
        assertEquals(requestBody, recordedRequest.utf8Body)
    }

    @test fun testPublish() {
        val requestBody = TestUtils.fileToString("entry_create_links_request.json")
        server!!.enqueue(MockResponse().setResponseCode(200).setBody(requestBody))

        assertTestCallback(client!!.entries().async().publish(CMAEntry()
                .setId("entryid")
                .setSpaceId("spaceid")
                .setVersion(1.0), TestCallback(true)) as TestCallback)

        // Request
        val recordedRequest = server!!.takeRequest()
        assertEquals("PUT", recordedRequest.method)
        assertEquals("/spaces/spaceid/entries/entryid/published", recordedRequest.path)
        assertNotNull(recordedRequest.getHeader("X-Contentful-Version"))
    }

    @test fun testUnArchive() {
        val requestBody = TestUtils.fileToString("space_create_request.json")
        server!!.enqueue(MockResponse().setResponseCode(200).setBody(requestBody))

        assertTestCallback(client!!.entries().async().unArchive(
                CMAEntry().setId("entryid").setSpaceId("spaceid"),
                TestCallback(true)) as TestCallback)

        // Request
        val recordedRequest = server!!.takeRequest()
        assertEquals("DELETE", recordedRequest.method)
        assertEquals("/spaces/spaceid/entries/entryid/archived", recordedRequest.path)
    }

    @test fun testUnPublish() {
        val requestBody = TestUtils.fileToString("entry_create_links_request.json")
        server!!.enqueue(MockResponse().setResponseCode(200).setBody(requestBody))

        assertTestCallback(client!!.entries().async().unPublish(
                CMAEntry().setId("entryid").setSpaceId("spaceid"),
                TestCallback(true)) as TestCallback)

        // Request
        val recordedRequest = server!!.takeRequest()
        assertEquals("DELETE", recordedRequest.method)
        assertEquals("/spaces/spaceid/entries/entryid/published", recordedRequest.path)
    }

    @test(expected = RuntimeException::class)
    fun testRetainsSysOnNetworkError() {
        val badClient = CMAClient.Builder()
                .setAccessToken("accesstoken")
                .setCoreCallFactory { throw RuntimeException(it.url().toString(), IOException()) }
                .build()

        val entry = CMAEntry().setVersion(31337.0)
        try {
            badClient.entries().create("spaceid", "ctid", entry)
        } catch (e: RuntimeException) {
            assertEquals(31337, entry.version)
            throw e
        }
    }

    @test(expected = Exception::class)
    fun testUpdateFailsWithoutVersion() {
        ModuleTestUtils.assertUpdateWithoutVersion {
            client!!.entries().update(CMAEntry().setId("eid").setSpaceId("spaceid"))
        }
    }
}
