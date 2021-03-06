/*
 * Copyright (C) 2017 Contentful GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

import com.contentful.java.cma.lib.TestCallback
import com.contentful.java.cma.lib.TestUtils
import junit.framework.TestCase.assertEquals
import okhttp3.mockwebserver.MockResponse
import org.hamcrest.CoreMatchers.startsWith
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test as test

class UploadTests : BaseTest() {
    @test
    fun testGetUploadMessage() {
        val responseBody = TestUtils.fileToString("upload_welcome_response.json")
        server!!.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val result = assertTestCallback(client!!.uploads().async()
                .welcome(TestCallback()) as TestCallback)!!

        // Request
        val recordedRequest = server!!.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertEquals("/", recordedRequest.path)
        assertThat(result.message, startsWith("Read the docs: https://www.contentful.com"))
        assertEquals("Welcome", result.sys["type"])
    }

    @test
    fun testPostBinaryToUpload() {
        val responseBody = TestUtils.fileToString("upload_post_response.json")
        server!!.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val uploadStream = TestUtils.fileToInputStream("upload_post_payload.jpg")

        val result = assertTestCallback(client!!.uploads().async()
                .create("space_id", uploadStream, TestCallback()) as TestCallback)!!

        // Request
        val recordedRequest = server!!.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/spaces/space_id/uploads", recordedRequest.path)
        assertEquals("upload_id", result.resourceId)
    }

    @test
    fun testGetUploadWithId() {
        val responseBody = TestUtils.fileToString("upload_get_upload_with_id.json")
        server!!.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val result = assertTestCallback(client!!.uploads().async()
                .fetchOne("space_id", "upload_id", TestCallback()) as TestCallback)!!

        // Request
        val recordedRequest = server!!.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertEquals("/spaces/space_id/uploads/upload_id", recordedRequest.path)

        assertEquals("upload_id", result.sys["id"])
        assertEquals("Upload", result.sys["type"])
    }

    @test
    fun testDeleteUpload() {
        server!!.enqueue(MockResponse().setResponseCode(204))

        val result = assertTestCallback(client!!.uploads().async()
                .delete("space_id", "upload_id", TestCallback()) as TestCallback)!!

        // Request
        val recordedRequest = server!!.takeRequest()
        assertEquals("DELETE", recordedRequest.method)
        assertEquals("/spaces/space_id/uploads/upload_id", recordedRequest.path)
        assertEquals(204, result)
    }
}
