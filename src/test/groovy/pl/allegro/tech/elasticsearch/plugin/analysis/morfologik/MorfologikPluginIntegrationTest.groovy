package pl.allegro.tech.elasticsearch.plugin.analysis.morfologik

import org.apache.http.HttpHost
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.AnalyzeRequest
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.MINUTES
import static pl.allegro.tech.elasticsearch.plugin.analysis.morfologik.AnalysisMorfologikPlugin.ANALYZER_NAME
import static pl.allegro.tech.elasticsearch.plugin.analysis.morfologik.AnalysisMorfologikPlugin.FILTER_NAME
import static pl.allegro.tech.embeddedelasticsearch.PopularProperties.HTTP_PORT
import static pl.allegro.tech.embeddedelasticsearch.PopularProperties.TRANSPORT_TCP_PORT

class MorfologikPluginIntegrationTest extends Specification {

    static final String ELASTIC_VERSION = System.properties['elasticsearchVersion']
    static final int ELS_PORT = 9301
    static final int ELS_HTTP_PORT = 9201

    static final String MORFOLOGIK_PLUGIN_PATH =
            "build/distributions/elasticsearch-analysis-morfologik-" + ELASTIC_VERSION + ".zip"

    static final embeddedElastic = EmbeddedElastic.builder()
            .withEsJavaOpts("-Xms128m -Xmx512m")
            .withElasticVersion(ELASTIC_VERSION)
            .withSetting(TRANSPORT_TCP_PORT, ELS_PORT)
            .withSetting(HTTP_PORT, ELS_HTTP_PORT)
            .withPlugin(new File(MORFOLOGIK_PLUGIN_PATH).toURI().toURL().toString())
            .withStartTimeout(1, MINUTES)
            .build()
            .start()

    static final elasticsearchClient = createClient()

    def cleanupSpec() {
        embeddedElastic.stop()
    }

    def "morfologik analyzer should work"() {
        expect:
        analyzeAndGetFirstTermResult(AnalyzeRequest.withGlobalAnalyzer(ANALYZER_NAME, "jestem")) == "być"
    }

    def "morfologik token filter should work"() {
        expect:
        analyzeAndGetFirstTermResult(AnalyzeRequest.buildCustomAnalyzer("standard")
                                             .addTokenFilter(FILTER_NAME)
                                             .build("jestem")) == "być"
    }

    private static String analyzeAndGetFirstTermResult(AnalyzeRequest analyzeRequest) {
        def result = elasticsearchClient.indices().analyze(analyzeRequest, RequestOptions.DEFAULT)
        result.tokens.collect { it.term }.join(" ")
    }

    static def createClient() {
        def client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(InetAddress.loopbackAddress, ELS_HTTP_PORT, "http")))
        client
    }
}
