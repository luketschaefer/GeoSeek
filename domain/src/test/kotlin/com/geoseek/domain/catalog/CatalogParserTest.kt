package com.geoseek.domain.catalog

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class CatalogParserTest {
    private val parser = CatalogParser(minObjectsPerEnvironment = 1)

    private fun obj(
        id: String = "bench",
        name: String = "Bench",
        rarity: String = "COMMON",
        points: Int = 10,
        xp: Int = 5,
        environments: String = """["PARK","KITCHEN","STREET","CAMPUS"]""",
        labels: String = """["Bench"]""",
        minConfidence: Float = 0.7f,
    ) = """{"id":"$id","name":"$name","rarity":"$rarity","points":$points,"xp":$xp,""" +
        """"environments":$environments,"labels":$labels,"minConfidence":$minConfidence}"""

    private fun catalog(
        vararg objects: String,
        version: Int = 1,
    ) = """{"version":$version,"objects":[${objects.joinToString(",")}]}"""

    private fun invalidErrors(json: String): List<String> =
        assertThrows(CatalogException.Invalid::class.java) { parser.parse(json) }.errors

    @Test
    fun `parses valid catalog`() {
        val catalog = parser.parse(catalog(obj(), obj(id = "cup", name = "Cup", labels = """["Cup"]""")))
        assertThat(catalog.objects.map { it.id.value }).containsExactly("bench", "cup").inOrder()
        val bench = catalog.require(ObjectId("bench"))
        assertThat(bench.rarity).isEqualTo(Rarity.COMMON)
        assertThat(bench.acceptedLabels).containsExactly("Bench")
        assertThat(bench.environments).containsExactlyElementsIn(Environment.entries)
    }

    @Test
    fun `syntax error is malformed`() {
        assertThrows(CatalogException.Malformed::class.java) { parser.parse("""{"version":1,"objects":[""") }
    }

    @Test
    fun `unknown key is malformed`() {
        val json = catalog(obj().replace("\"minConfidence\"", "\"minConfidense\""))
        assertThrows(CatalogException.Malformed::class.java) { parser.parse(json) }
    }

    @Test
    fun `missing field is malformed`() {
        val json = catalog(obj().replace(""","xp":5""", ""))
        assertThrows(CatalogException.Malformed::class.java) { parser.parse(json) }
    }

    @Test
    fun `unknown rarity is malformed`() {
        assertThrows(CatalogException.Malformed::class.java) { parser.parse(catalog(obj(rarity = "MYTHIC"))) }
    }

    @Test
    fun `unknown environment is malformed`() {
        assertThrows(CatalogException.Malformed::class.java) {
            parser.parse(catalog(obj(environments = """["BEACH"]""")))
        }
    }

    @Test
    fun `wrong type is malformed`() {
        val json = catalog(obj().replace(""""points":10""", """"points":"ten""""))
        assertThrows(CatalogException.Malformed::class.java) { parser.parse(json) }
    }

    @Test
    fun `empty catalog is invalid`() {
        assertThat(invalidErrors(catalog())).contains("Catalog has no objects")
    }

    @Test
    fun `unsupported version is invalid`() {
        assertThat(invalidErrors(catalog(obj(), version = 2)).single()).contains("Unsupported catalog version 2")
    }

    @Test
    fun `duplicate ids are invalid`() {
        assertThat(invalidErrors(catalog(obj(), obj()))).contains("Duplicate id 'bench'")
    }

    @Test
    fun `reports every content problem at once`() {
        val bad =
            obj(
                id = "Bad Id",
                name = " ",
                points = 0,
                xp = -1,
                environments = "[]",
                labels = """[""]""",
                minConfidence = 1.5f,
            )
        val errors = invalidErrors(catalog(bad, obj()))
        assertThat(errors.joinToString("\n")).apply {
            contains("id must match")
            contains("name is blank")
            contains("points must be > 0")
            contains("xp must be > 0")
            contains("environments is empty")
            contains("labels contains a blank entry")
            contains("minConfidence must be in (0, 1]")
        }
    }

    @Test
    fun `zero confidence is invalid`() {
        assertThat(invalidErrors(catalog(obj(minConfidence = 0f))).single()).contains("minConfidence")
    }

    @Test
    fun `too few objects per environment is invalid`() {
        val strict = CatalogParser(minObjectsPerEnvironment = 2)
        val errors =
            assertThrows(CatalogException.Invalid::class.java) {
                strict.parse(catalog(obj()))
            }.errors
        assertThat(errors).hasSize(Environment.entries.size)
        assertThat(errors.first()).contains("at least 2 required")
    }

    @Test
    fun `unknown detector label is invalid when label map given`() {
        val withLabels = CatalogParser(minObjectsPerEnvironment = 1, knownLabels = setOf("Bench"))
        val errors =
            assertThrows(CatalogException.Invalid::class.java) {
                withLabels.parse(catalog(obj(labels = """["Bench","Park bench"]""")))
            }.errors
        assertThat(errors.single()).contains("label 'Park bench' is not in the detector's label map")
    }
}
