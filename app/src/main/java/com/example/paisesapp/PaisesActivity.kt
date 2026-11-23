package com.example.paisesapp

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.Normalizer

class PaisesActivity : AppCompatActivity() {

    private lateinit var rvPaises: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var etBuscarPais: EditText
    private lateinit var paisAdapter: PaisAdapter
    private var listaCompletaPaises: List<Pais> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paises)

        rvPaises = findViewById(R.id.rvPaises)
        progressBar = findViewById(R.id.progressBarPaises)
        etBuscarPais = findViewById(R.id.etBuscarPais)

        paisAdapter = PaisAdapter(emptyList())
        rvPaises.layoutManager = LinearLayoutManager(this)
        rvPaises.adapter = paisAdapter

        etBuscarPais.doOnTextChanged { texto, _, _, _ ->
            filtrarPaises(texto?.toString().orEmpty())
        }

        cargarPaisesDesdeApi()
    }

    private fun filtrarPaises(consulta: String) {
        val termino = normalizarTexto(consulta)
        val filtrados = if (termino.isEmpty()) {
            listaCompletaPaises
        } else {
            listaCompletaPaises.filter { pais ->
                normalizarTexto(pais.nombre).contains(termino)
            }
        }
        paisAdapter.actualizarLista(filtrados)
    }

    private fun normalizarTexto(texto: String): String {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
            .trim()
    }

    private fun cargarPaisesDesdeApi() {
        progressBar.visibility = View.VISIBLE

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.restcountries.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val servicio = retrofit.create(RestCountriesApi::class.java)
        cargarPagina(servicio, offset = 0, acumulado = mutableListOf())
    }

    private fun cargarPagina(
        servicio: RestCountriesApi,
        offset: Int,
        acumulado: MutableList<Pais>
    ) {
        servicio.obtenerPaises(limit = 100, offset = offset).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (response.isSuccessful && response.body() != null) {
                    val arreglo = extraerArregloPaises(response.body()!!)
                    if (arreglo != null) {
                        val pagina = arreglo.mapNotNull { el ->
                            if (el.isJsonObject) parsearPais(el.asJsonObject) else null
                        }
                        acumulado.addAll(pagina)

                        if (arreglo.size() == 100) {
                            cargarPagina(servicio, offset + 100, acumulado)
                        } else {
                            mostrarListaOrdenada(acumulado)
                        }
                    } else if (acumulado.isNotEmpty()) {
                        mostrarListaOrdenada(acumulado)
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(
                            this@PaisesActivity,
                            "Respuesta sin datos de países",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else if (acumulado.isNotEmpty()) {
                    mostrarListaOrdenada(acumulado)
                } else {
                    progressBar.visibility = View.GONE
                    val errorTexto = response.errorBody()?.string() ?: "Código ${response.code()}"
                    Toast.makeText(
                        this@PaisesActivity,
                        "Error HTTP: $errorTexto",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                if (acumulado.isNotEmpty()) {
                    mostrarListaOrdenada(acumulado)
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@PaisesActivity,
                        "Error de red: ${t.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    }

    private fun mostrarListaOrdenada(lista: List<Pais>) {
        progressBar.visibility = View.GONE
        val destacados = listOf("Guatemala", "El Salvador", "Honduras")
        listaCompletaPaises = lista.sortedWith(
            compareBy<Pais> { pais ->
                val idx = destacados.indexOf(pais.nombre)
                if (idx == -1) destacados.size else idx
            }.thenBy { it.nombre }
        )
        filtrarPaises(etBuscarPais.text?.toString().orEmpty())
    }

    private fun extraerArregloPaises(json: JsonElement): JsonArray? {
        if (json.isJsonArray) return json.asJsonArray
        if (!json.isJsonObject) return null

        val raiz = json.asJsonObject
        val dataElem = raiz.get("data")
        if (dataElem != null) {
            if (dataElem.isJsonArray) return dataElem.asJsonArray
            if (dataElem.isJsonObject) {
                return dataElem.asJsonObject.entrySet()
                    .firstOrNull { it.value.isJsonArray }?.value?.asJsonArray
            }
        }
        return raiz.entrySet().firstOrNull { it.value.isJsonArray }?.value?.asJsonArray
    }

    private fun parsearPais(obj: JsonObject): Pais? {
        val namesObj = obj.get("names")?.takeIf { it.isJsonObject }?.asJsonObject

        val translationsObj = namesObj?.get("translations")?.takeIf { it.isJsonObject }?.asJsonObject
            ?: obj.get("translations")?.takeIf { it.isJsonObject }?.asJsonObject

        val nombreEspanol = listOf("spa", "es").firstNotNullOfOrNull { clave ->
            val item = translationsObj?.get(clave)
            when {
                item == null -> null
                item.isJsonPrimitive -> item.asString.takeIf { it.isNotBlank() }
                item.isJsonObject -> item.asJsonObject.get("common")
                    ?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
                else -> null
            }
        }

        val nombreComun = namesObj?.get("common")?.takeIf { it.isJsonPrimitive }?.asString
            ?: obj.get("name")?.takeIf { it.isJsonPrimitive }?.asString
            ?: ""

        val nombreFinal = nombreEspanol ?: nombreComun.takeIf { it.isNotBlank() } ?: return null

        val urlDirecta = extraerUrlBanderaDirecta(obj)
        val codigoIso2 = extraerCodigoIso2(obj)

        val urlBandera = urlDirecta
            ?: codigoIso2?.let { "https://flags.restcountries.com/v5/w320/${it.lowercase()}.png" }
            ?: ""

        return Pais(nombre = nombreFinal, banderaUrl = urlBandera)
    }

    private fun extraerUrlBanderaDirecta(elemento: JsonElement?): String? {
        if (elemento == null || elemento.isJsonNull) return null
        if (elemento.isJsonPrimitive) {
            val texto = elemento.asString.trim()
            if (texto.startsWith("http") &&
                (texto.contains("flags.restcountries.com") || texto.endsWith(".png") || texto.endsWith(".jpg"))
            ) {
                return texto
            }
            return null
        }
        if (elemento.isJsonObject) {
            val obj = elemento.asJsonObject
            for (clavePreferida in listOf("flags", "flag", "png", "w320", "w160", "w640", "url", "default")) {
                val encontrada = extraerUrlBanderaDirecta(obj.get(clavePreferida))
                if (encontrada != null) return encontrada
            }
            for ((clave, valor) in obj.entrySet()) {
                if (clave == "names" || clave == "svg") continue
                val encontrada = extraerUrlBanderaDirecta(valor)
                if (encontrada != null) return encontrada
            }
        }
        return null
    }

    private fun extraerCodigoIso2(obj: JsonObject): String? {
        val clavesIso = listOf(
            "alpha_2", "cca2", "iso2", "alpha2", "iso_alpha2",
            "iso_3166_1_alpha_2", "country_code", "code"
        )
        for (k in clavesIso) {
            val v = obj.get(k)?.takeIf { it.isJsonPrimitive }?.asString?.trim()
            if (v != null && v.length == 2 && v.all { it.isLetter() }) return v
        }
        for ((claveContenedor, valorContenedor) in obj.entrySet()) {
            if (claveContenedor == "names" || !valorContenedor.isJsonObject) continue
            val subObj = valorContenedor.asJsonObject
            for (k in clavesIso) {
                val v = subObj.get(k)?.takeIf { it.isJsonPrimitive }?.asString?.trim()
                if (v != null && v.length == 2 && v.all { it.isLetter() }) return v
            }
        }
        return null
    }
}