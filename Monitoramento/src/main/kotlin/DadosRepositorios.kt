import com.fasterxml.jackson.databind.ObjectMapper
import com.github.britooo.looca.api.core.Looca
import com.github.britooo.looca.api.group.janelas.Janela
import com.github.britooo.looca.api.group.processos.Processo
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForList
import org.springframework.jdbc.core.queryForObject
import java.time.LocalDate
import java.time.LocalDateTime

class DadosRepositorios {

    lateinit var jdbcTemplate: JdbcTemplate
    lateinit var jdbcTemplate_server: JdbcTemplate

    fun iniciar(){
        jdbcTemplate = Conexao().conectar()
    }

    fun iniciar_server() {
        jdbcTemplate_server = Conexao().conectar_server()
    }

    fun cadastrarJanela(novaJanela: MutableList<Janela>?, id_maquina: Int, fk_empresa: Int) {
        val janelasNoBanco = jdbcTemplate.queryForList(
            "SELECT nome_janela FROM janela where fk_maquinaJ = $id_maquina and fk_empresaJ = $fk_empresa",
            String::class.java)

        val janelasNoBancoServer = jdbcTemplate_server.queryForList(
                "SELECT nome_janela FROM janela where fk_maquinaJ = $id_maquina and fk_empresaJ = $fk_empresa",
        String::class.java
        )

        val janelasListadas = novaJanela?.filter { it.titulo != null && it.titulo.isNotBlank() }?.map { it.titulo }

        novaJanela?.forEach { janela ->
            if (janela.titulo != null && janela.titulo.isNotBlank()) {
                val janelaExisteNoBanco = janelasNoBanco.contains(janela.titulo)


                if (janelaExisteNoBanco) {
                    // A janela existe no banco, atualize-a definindo status_abertura como verdadeiro.
                    jdbcTemplate.update(
                        """
                UPDATE janela
                SET data_hora = ?,
                    status_abertura = ?
                WHERE nome_janela = ? AND fk_maquinaJ = $id_maquina AND fk_empresaJ = $fk_empresa
                """,
                        LocalDateTime.now(),
                        true,
                        janela.titulo
                    )
                } else {
                    // A janela não existe no banco, insira-a com status_abertura como verdadeiro.
                    jdbcTemplate.update(
                        """
                INSERT INTO janela (nome_janela, data_hora, status_abertura, fk_maquinaJ, fk_empresaJ)
                VALUES (?, ?, ?, $id_maquina, $fk_empresa)
                """,
                        janela.titulo,
                        LocalDateTime.now(),
                        true
                    )
                }

                val janelaExisteNoBancoServer = janelasNoBancoServer.contains(janela.titulo)

                if (janelaExisteNoBancoServer) {
                    // A janela existe no banco, atualize-a definindo status_abertura como verdadeiro.
                    jdbcTemplate_server.update(
                        """
                UPDATE janela
                SET data_hora = ?,
                    status_abertura = ?
                WHERE nome_janela = ? AND fk_maquinaJ = $id_maquina AND fk_empresaJ = $fk_empresa
                """,
                        LocalDateTime.now(),
                        true,
                        janela.titulo
                    )
                } else {
                    // A janela não existe no banco, insira-a com status_abertura como verdadeiro.
                    jdbcTemplate_server.update(
                        """
                INSERT INTO janela (nome_janela, data_hora, status_abertura, fk_maquinaJ, fk_empresaJ)
                VALUES (?, ?, ?, $id_maquina, $fk_empresa)
                """,
                        janela.titulo,
                        LocalDateTime.now(),
                        true
                    )
                }
            }
        }

        if (janelasListadas != null && janelasListadas.isNotEmpty()) {
            val placeholders = janelasListadas.map { "?" }.joinToString(", ")
            val updateQuery = "UPDATE janela SET status_abertura = ? WHERE nome_janela NOT IN ($placeholders)"
            val params = arrayOf(false, *janelasListadas.toTypedArray())
            val queryJanela = jdbcTemplate.update(updateQuery, *params)
            val queryJanelaServer = jdbcTemplate_server.update(updateQuery, *params)
            println("${queryJanela + queryJanelaServer} registros atualizados na tabela de janelas")
        }
    }

    fun cadastrarRede(novaRede: Redes, id_maquina: Int, fk_empresa: Int) {

        var rowBytesEnviados = jdbcTemplate.update(
            """
                insert into monitoramento (dado_coletado, data_hora, descricao, fk_componentes_monitoramento, fk_maquina_monitoramento, fk_empresa_monitoramento, fk_unidade_medida) values
                (?,?,'bytes enviados',8,$id_maquina,$fk_empresa,1)
            """,
            novaRede.bytesEnviados,
            novaRede.dataHora
        )

        var rowBytesRecebidos = jdbcTemplate.update(
            """
                insert into monitoramento (dado_coletado, data_hora, descricao, fk_componentes_monitoramento, fk_maquina_monitoramento, fk_empresa_monitoramento, fk_unidade_medida) values
                (?,?,'bytes recebidos',8,$id_maquina,$fk_empresa,1)
            """,
            novaRede.bytesRecebidos,
            novaRede.dataHora
        )

        var rowBytesEnviadosServer = jdbcTemplate_server.update(
            """
                insert into monitoramento (dado_coletado, data_hora, descricao, fk_componentes_monitoramento, fk_maquina_monitoramento, fk_empresa_monitoramento, fk_unidade_medida) values
                (?,?,'bytes enviados',(SELECT id_componente from componente WHERE nome_componente = 'REDE' and fk_maquina_componente = $id_maquina),$id_maquina,$fk_empresa,1)
            """,
            novaRede.bytesEnviados,
            novaRede.dataHora
        )

        var rowBytesRecebidosServer = jdbcTemplate_server.update(
            """
                insert into monitoramento (dado_coletado, data_hora, descricao, fk_componentes_monitoramento, fk_maquina_monitoramento, fk_empresa_monitoramento, fk_unidade_medida) values
                (?,?,'bytes recebidos',(SELECT id_componente from componente WHERE nome_componente = 'REDE' and fk_maquina_componente = $id_maquina),$id_maquina,$fk_empresa,1)
            """,
            novaRede.bytesRecebidos,
            novaRede.dataHora
        )

        println(
            """
            ${rowBytesEnviados + rowBytesEnviadosServer} query de bytes enviados foi registrado no banco
            ${rowBytesRecebidos + rowBytesRecebidosServer} query de bytes recebidos foi registrado no banco
        """.trimIndent()
        )

    }

    fun cadastrarRAM(novaRam: Long, id_maquina: Int, fk_empresa: Int) {

        var usoRam = jdbcTemplate.update(
            """
                insert into monitoramento (dado_coletado, data_hora, descricao, fk_componentes_monitoramento, fk_maquina_monitoramento, fk_empresa_monitoramento, fk_unidade_medida) values
                (?,?,'uso de ram kt',(SELECT id_componente from componente WHERE nome_componente = 'RAM' and fk_maquina_componente = $id_maquina),$id_maquina,$fk_empresa,1)
            """,
            novaRam,
            LocalDateTime.now()
        )

        var usoRamServer = jdbcTemplate_server.update(
            """
                insert into monitoramento (dado_coletado, data_hora, descricao, fk_componentes_monitoramento, fk_maquina_monitoramento, fk_empresa_monitoramento, fk_unidade_medida) values
                (?,?,'uso de ram kt',(SELECT id_componente from componente WHERE nome_componente = 'RAM' and fk_maquina_componente = $id_maquina),$id_maquina,$fk_empresa,1)
            """,
            novaRam,
            LocalDateTime.now()
        )

        println(
            """
            ${usoRam + usoRamServer} query de uso de ram foi registrado no banco
        """.trimIndent()
        )

    }

    fun cadastrarProcesso(novoProcesso: MutableList<Processo>?, id_maquina: Int, fk_empresa: Int) {
        val processosNoBanco = jdbcTemplate.queryForList(
            "SELECT pid FROM processos where fk_maquinaP = $id_maquina and fk_empresaP = $fk_empresa",
            Int::class.java
        )

        val pidsListados = novoProcesso?.map { it.pid }

        novoProcesso?.forEach { p ->
            if (p.pid != null && (pidsListados == null || pidsListados.contains(p.pid))) {
                val validacao = validarProcesso(p.pid, id_maquina, fk_empresa)

                if (validacao) {
                    val queryProcesso = jdbcTemplate.update(
                        """
                    INSERT INTO processos (PID, nome_processo, uso_cpu, uso_memoria, memoria_virtual, status_abertura, fk_maquinaP, fk_empresaP, data_hora)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                        p.pid,
                        p.nome,
                        p.usoCpu,
                        p.usoMemoria,
                        p.memoriaVirtualUtilizada,
                        true,
                        id_maquina,
                        fk_empresa,
                        LocalDateTime.now()
                    )
                    println("$queryProcesso registro inserido na tabela de processos")
                }

                val validacaoServer = validarProcessoServer(p.pid, id_maquina, fk_empresa)
                if (validacaoServer) {
                    val queryProcesso = jdbcTemplate_server.update(
                        """
                    INSERT INTO processos (PID, nome_processo, uso_cpu, uso_memoria, memoria_virtual, status_abertura, fk_maquinaP, fk_empresaP, data_hora)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                        p.pid,
                        p.nome,
                        p.usoCpu,
                        p.usoMemoria,
                        p.memoriaVirtualUtilizada,
                        true,
                        id_maquina,
                        fk_empresa,
                        LocalDateTime.now()
                    )
                    println("$queryProcesso registro inserido na tabela de processos do server")
                }
            }
        }

        if (pidsListados != null && pidsListados.isNotEmpty()) {
            val placeholders = pidsListados.map { "?" }.joinToString(", ")
            val updateQuery = "UPDATE processos SET status_abertura = false WHERE PID NOT IN ($placeholders) and fk_maquinaP = $id_maquina"
            val queryProcesso = jdbcTemplate.update(updateQuery, *pidsListados.toTypedArray())
            println("$queryProcesso registros atualizados na tabela de processos")
        }
    }

    fun validarProcesso(pid: Int, id_maquina: Int, fk_empresa: Int): Boolean {
        val queryValidacao = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM processos WHERE pid = ? and fk_maquinaP = $id_maquina and fk_empresaP = $fk_empresa",
            Int::class.java,
            pid
        )
        return queryValidacao > 0
    }

    fun validarProcessoServer(pid: Int, id_maquina: Int, fk_empresa: Int): Boolean {
        val queryValidacao = jdbcTemplate_server.queryForObject(
            "SELECT count(*) FROM processos WHERE pid = ? and fk_maquinaP = $id_maquina and fk_empresaP = $fk_empresa",
            Int::class.java,
            pid
        )
        return queryValidacao > 0
    }

    fun capturarDadosJ(looca: Looca): MutableList<Janela>? {
        val janela = looca.grupoDeJanelas
        var janelasVisiveis = janela.janelasVisiveis

        return janelasVisiveis
    }

    fun capturarDadosR(looca: Looca): Redes {
        val redes = looca.rede.grupoDeInterfaces.interfaces

        val listaBytesRecebidos = mutableListOf<Long>()
        val listaBytesEnviados = mutableListOf<Long>()


        for (rede in redes) {
            listaBytesRecebidos.add(rede.getBytesRecebidos())
            listaBytesEnviados.add(rede.getBytesEnviados())
        }

        val nomeRede = "eth15"

        return Redes(0, LocalDateTime.now(), nomeRede, listaBytesEnviados.max(), listaBytesRecebidos.max())

    }

    fun capturarDadosP(looca: Looca): MutableList<Processo>? {
        val processos = looca.grupoDeProcessos
        var listaProcessos = processos.processos
        return listaProcessos
    }

    fun capturarDadosRam(looca: Looca): Long{
        val ram = looca.memoria
        var uso_ram = ram.emUso
        return uso_ram
    }

}


