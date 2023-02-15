package com.example.oa_wallet_android

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException

data class GetUploadurlResponse(
    val filename: String,
    val upload_url: String
)

data class GetDownloadurlRequestParams(
    val filename: String,
    val expiry_in_seconds: Int
)

data class GetDownloadurlResponse(
    val download_url: String
)
class DocumentsService {
    companion object {
        fun uploadDocument(document: String,validityDuration: Int) : String  {
            val client = OkHttpClient()
            var gson = Gson()
            val uploadEndPoint = Config.getuploadurlEndpoint
            val downloadEndPoint = Config.getdownloadurlEndpoint
            var getUploadurlResponse: GetUploadurlResponse?
            var getDownloadurlResponse: GetDownloadurlResponse?

            // Get the S3 presigned upload url
            val getUploadurlRequest = Request.Builder()
                .url(uploadEndPoint)
                .build()

            client.newCall(getUploadurlRequest).execute().use { response ->
                if (!response.isSuccessful) throw IOException("getuploadurl: unexpected response code $response")
                getUploadurlResponse = gson.fromJson(response.body!!.string(),GetUploadurlResponse::class.java)
            }

            // Upload the document to S3
            val uploadRequest = Request.Builder()
                .url(getUploadurlResponse!!.upload_url)
                .put(document.toRequestBody())
                .build()

            client.newCall(uploadRequest).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Upload file unexpected response code $response")
            }

            // Get the S3 presigned download url for the document
            val downloadUrlParams = GetDownloadurlRequestParams(getUploadurlResponse!!.filename, validityDuration)
            val downloadUrlParamsJsonObj = gson.toJson(downloadUrlParams)

            val getDownloadurlRequest = Request.Builder()
                .url(downloadEndPoint)
                .post(downloadUrlParamsJsonObj.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(getDownloadurlRequest).execute().use { response ->
                if (!response.isSuccessful) throw IOException("getdownloadurl unexpected response code $response")
                getDownloadurlResponse = gson.fromJson(response.body!!.string(),GetDownloadurlResponse::class.java)
            }

            return getDownloadurlResponse!!.download_url
        }
    }
}