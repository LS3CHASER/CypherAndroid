package com.shannon.cypher.weather.alerts

import android.text.Html
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element


data class CypherBomWarning(
    val identifier: String,
    val title: String,
    val description: String,
    val link: String,
    val published: String,
)


class CypherBomWarningClient {

    companion object {

        private const val CONNECT_TIMEOUT_MS =
            8_000

        private const val READ_TIMEOUT_MS =
            10_000
    }


    fun getWarnings(
        feedUrl: String,
    ): List<CypherBomWarning> {

        val connection =
            URL(
                feedUrl
            )
                .openConnection() as
                    HttpURLConnection


        try {

            connection
                .requestMethod =
                "GET"

            connection
                .connectTimeout =
                CONNECT_TIMEOUT_MS

            connection
                .readTimeout =
                READ_TIMEOUT_MS

            connection
                .setRequestProperty(
                    "User-Agent",
                    "Cypher/1.0 Android personal weather alert client",
                )

            connection
                .setRequestProperty(
                    "Accept",
                    "application/rss+xml, application/xml, text/xml",
                )


            val responseCode =
                connection
                    .responseCode


            if (
                responseCode !in
                200..299
            ) {

                throw IllegalStateException(
                    "BOM warning feed returned HTTP $responseCode."
                )
            }


            connection
                .inputStream
                .use {
                        inputStream ->

                    val factory =
                        DocumentBuilderFactory
                            .newInstance()


                    factory
                        .isNamespaceAware =
                        false


                    /*
                     * Prevent external entity expansion.
                     */
                    runCatching {
                        factory.setFeature(
                            "http://apache.org/xml/features/disallow-doctype-decl",
                            true,
                        )
                    }

                    runCatching {
                        factory.setFeature(
                            "http://xml.org/sax/features/external-general-entities",
                            false,
                        )
                    }

                    runCatching {
                        factory.setFeature(
                            "http://xml.org/sax/features/external-parameter-entities",
                            false,
                        )
                    }


                    val document =
                        factory
                            .newDocumentBuilder()
                            .parse(
                                inputStream
                            )


                    val items =
                        document
                            .getElementsByTagName(
                                "item"
                            )


                    val results =
                        mutableListOf<CypherBomWarning>()


                    for (
                    index in
                    0 until
                            items.length
                    ) {

                        val element =
                            items
                                .item(
                                    index
                                ) as?
                                    Element
                                ?: continue


                        val title =
                            childText(
                                element,
                                "title",
                            )


                        val description =
                            plainText(
                                childText(
                                    element,
                                    "description",
                                )
                            )


                        val link =
                            childText(
                                element,
                                "link",
                            )


                        val guid =
                            childText(
                                element,
                                "guid",
                            )


                        val published =
                            childText(
                                element,
                                "pubDate",
                            )


                        if (
                            title.isBlank()
                        ) {

                            continue
                        }


                        results.add(
                            CypherBomWarning(
                                identifier =
                                    guid.ifBlank {
                                        link.ifBlank {
                                            title
                                        }
                                    },

                                title =
                                    plainText(
                                        title
                                    ),

                                description =
                                    description,

                                link =
                                    link,

                                published =
                                    published,
                            )
                        )
                    }


                    return results
                }

        } finally {

            connection
                .disconnect()
        }
    }


    private fun childText(
        element: Element,
        tagName: String,
    ): String {

        return element
            .getElementsByTagName(
                tagName
            )
            .item(
                0
            )
            ?.textContent
            ?.trim()
            .orEmpty()
    }


    private fun plainText(
        value: String,
    ): String {

        if (
            value.isBlank()
        ) {

            return ""
        }


        @Suppress("DEPRECATION")
        return Html
            .fromHtml(
                value
            )
            .toString()
            .replace(
                Regex(
                    "\\s+"
                ),
                " ",
            )
            .trim()
    }
}
