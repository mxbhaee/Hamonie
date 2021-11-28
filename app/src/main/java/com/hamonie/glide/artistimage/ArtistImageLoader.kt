package com.hamonie.glide.artistimage

import android.content.Context
import com.hamonie.network.DeezerService
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.InputStream
import java.util.concurrent.TimeUnit

class ArtistImageLoader(
    val context: Context,
    private val deezerService: DeezerService,
    private val okhttp: OkHttpClient
) : ModelLoader<ArtistImage, InputStream> {

    override fun buildLoadData(
        model: ArtistImage,
        width: Int,
        height: Int,
        options: Options
    ): LoadData<InputStream> {
        return LoadData(
            ObjectKey(model.artist.name),
            ArtistImageFetcher(context, deezerService, model, okhttp)
        )
    }

    override fun handles(model: ArtistImage): Boolean {
        return true
    }
}

class Factory(
    val context: Context
) : ModelLoaderFactory<ArtistImage, InputStream> {

    private var deezerService: DeezerService
    private var okHttp: OkHttpClient

    init {
        okHttp =
            OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .build()
        deezerService = DeezerService.invoke(
            DeezerService.createDefaultOkHttpClient(context)
                .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .addInterceptor(createLogInterceptor())
                .build()
        )
    }

    private fun createLogInterceptor(): Interceptor {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return interceptor
    }

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<ArtistImage, InputStream> {
        return ArtistImageLoader(
            context,
            deezerService,
            okHttp
        )
    }

    override fun teardown() {}

    companion object {
        // we need these very low values to make sure our artist image loading calls doesn't block the image loading queue
        private const val TIMEOUT: Long = 500
    }
}
