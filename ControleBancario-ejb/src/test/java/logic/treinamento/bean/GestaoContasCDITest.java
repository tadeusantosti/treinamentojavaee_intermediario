package logic.treinamento.bean;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;
import javax.inject.Inject;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import logic.treinamento.dao.InterfaceContaCorrente;
import logic.treinamento.dao.InterfaceLancamentoDao;
import logic.treinamento.model.AgenciaEnum;
import logic.treinamento.model.BancoEnum;
import logic.treinamento.model.TipoLancamentoEnum;
import logic.treinamento.model.ContaCorrente;
import logic.treinamento.model.Lancamento;
import logic.treinamento.observer.EventosGestaoContas;
import logic.treinamento.request.CadastroContaCorrenteRequisicao;
import logic.treinamento.request.LancamentoBancarioAtualizacaoRequisicao;
import logic.treinamento.request.LancamentoBancarioRequisicao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import utilitarios.Formatadores;

@RunWith(WeldJUnit4Runner.class)
public class GestaoContasCDITest {

    @Inject
    public InterfaceGestaoContas gestaoContaBean;

    @Inject
    InterfaceLancamentoDao gestaoContasDao;

    @Inject
    EventosGestaoContas eventosGestaoContas;

    @Inject
    InterfaceContaCorrente contaCorrenteDao;

    @Inject
    RastreioLancamentoBancarioMovimentacaoLocal rastreioBean;

    @Before
    public void setup() throws Exception {

        List<Lancamento> registrosPararemoverAntesTeste = gestaoContaBean.pesquisarLancamentoBancarioPorObservacao("Albert");
        if (!registrosPararemoverAntesTeste.isEmpty()) {
            for (Lancamento lancamento : registrosPararemoverAntesTeste) {
                eventosGestaoContas.excluirLancamentoBancario(lancamento.getId());
            }
        }

        List<Lancamento> registrosPararemoverAntesTeste1 = gestaoContaBean.pesquisarLancamentoBancarioPorObservacao("Charles Darwin");
        if (!registrosPararemoverAntesTeste1.isEmpty()) {
            for (Lancamento lancamento : registrosPararemoverAntesTeste1) {
                eventosGestaoContas.excluirLancamentoBancario(lancamento.getId());
            }
        }

        List<ContaCorrente> registrosContaCorrente = contaCorrenteDao.pesquisarTodasContasCorrentes();
        if (!registrosContaCorrente.isEmpty()) {
            for (ContaCorrente contaCorrente : registrosContaCorrente) {
                contaCorrenteDao.excluirContaCorrente(contaCorrente.getId());
            }
        }
    }

    /** <H3>Teste de Criacao e Consulta de um novo Lancamento Bancario</H3>
     * <br>
     * <br>
     * <p>
     * Objetivo do teste: Garantir que o sistema esteja criando o lancamento
     * bancario corretamente e que a consulta do lancamento atraves de parte da
     * observacao esteja funcionando adequadamente.</p>
     * <br>
     * <p>
     * <b>Configuração inicial para a realização dos testes: </b> <br>
     * Foi informado todos os campos principais necessarios para a correta
     * inclusao dos dados.</p>
     * <br>
     * <p>
     * <b>Relação de cenários com sua descrição, os passos executados e os
     * resultados esperados. *</b> </p>
     * <ul>
     * <li> <i> Cenário 1: Cadastrar um novo lancamento bancario. <i><br>
     * Resultado esperado: Sistema casdastrou o novo lancamento bancario
     * corretamente<br>
     * <li> <i> Cenário 2: Consultar o lancamento bancario que foi criado
     * atraves do campo observacao.
     * <i> <li>
     * Resultado esperado: Sistema consultou o registro com sucesso. <br>
     * </ul>
     * <br>
     * <p>
     * @since 1.0
     * @author Tadeu
     * @version 2.0 </p>
     */
    @Test
    public void testSalvarLancamentoBancario() throws Exception {

        CadastroContaCorrenteRequisicao cc = new CadastroContaCorrenteRequisicao();
        cc.setAgencia(AgenciaEnum.ARARAS.getId());
        cc.setBanco(BancoEnum.BRADESCO.getId());
        cc.setTitular("Son Gohan");
        eventosGestaoContas.salvarContaCorrente(cc);

        List<ContaCorrente> contas = contaCorrenteDao.pesquisarTodasContasCorrentes();

        if (!contas.isEmpty()) {
            for (ContaCorrente conta : contas) {
                assertTrue(BigDecimal.ZERO.compareTo(conta.getSaldo()) == 0);
                assertEquals(cc.getAgencia(), conta.getAgencia().getId());
                assertEquals(cc.getBanco(), conta.getBanco().getId());
                assertEquals(cc.getTitular(), conta.getTitular());
                assertTrue(conta.isSituacao());
            }
        } else {
            fail("A conta corrente nao foi cadastrada!");
        }

        LancamentoBancarioRequisicao lancRequisicao = new LancamentoBancarioRequisicao();
        lancRequisicao.setObservacao("Deposito na conta corrente do Albert Einstein");
        lancRequisicao.setValor(new BigDecimal(1234.56));
        lancRequisicao.setData(Formatadores.formatoDataInterface.format(new java.util.Date()));
        lancRequisicao.setIdTipoLancamento(TipoLancamentoEnum.DEPOSITO.getId());
        lancRequisicao.setIdContaCorrente(contas.get(0).getId());
        eventosGestaoContas.salvarLacamentoBancario(lancRequisicao);

        List<Lancamento> lancNovo = gestaoContaBean.pesquisarLancamentoBancarioPorObservacao("Albert");

        if (!lancNovo.isEmpty()) {
            for (Lancamento lancamentoConsultado : lancNovo) {
                assertEquals(lancRequisicao.getObservacao(), lancamentoConsultado.getObservacao());
                assertEquals(lancRequisicao.getValor().doubleValue(), lancamentoConsultado.getValor().doubleValue());
                assertEquals(lancRequisicao.getData(), Formatadores.formatoDataInterface.format(lancamentoConsultado.getData()));
                assertEquals(TipoLancamentoEnum.getByCodigo(lancRequisicao.getIdTipoLancamento()), lancamentoConsultado.getTipoLancamento());
            }
        } else {
            fail("O lancamento bancario nao foi salvo!");
        }
    }

    /** <H3>Teste de Atualizacao de um Lancamento Bancario cadastrado</H3>
     * <br>
     * <br>
     * <p>
     * Objetivo do teste: Garantir que o sistema esteja atualizando os dados de
     * um lancamento bancario corretamente.</p>
     * <br>
     * <p>
     * <b>Configuração inicial para a realização dos testes: </b> <br>
     * Foi criado um lancamento bancario e em seguida alterado algumas
     * informacoes do mesmo para que seja atualizado.</p>
     * <br>
     * <p>
     * <b>Relação de cenários com sua descrição, os passos executados e os
     * resultados esperados. *</b> </p>
     * <ul>
     * <li> <i> Cenário 1: Atualizar um novo lancamento bancario. <i><br>
     * Resultado esperado: Sistema atualizou os dados do lancamento bancario com
     * sucesso<br>
     * </ul>
     * <br>
     * <p>
     * @since 1.0
     * @author Tadeu
     * @version 2.0 </p>
     */
    @Test
    public void testAtualizarDadosLancamentoBancario() throws Exception {
        CadastroContaCorrenteRequisicao cc = new CadastroContaCorrenteRequisicao();
        cc.setAgencia(AgenciaEnum.ARARAS.getId());
        cc.setBanco(BancoEnum.BRADESCO.getId());
        cc.setTitular("Son Gohan");
        eventosGestaoContas.salvarContaCorrente(cc);

        List<ContaCorrente> contas = contaCorrenteDao.pesquisarTodasContasCorrentes();

        if (!contas.isEmpty()) {
            for (ContaCorrente conta : contas) {
                assertTrue(BigDecimal.ZERO.compareTo(conta.getSaldo()) == 0);
                assertEquals(cc.getAgencia(), conta.getAgencia().getId());
                assertEquals(cc.getBanco(), conta.getBanco().getId());
                assertEquals(cc.getTitular(), conta.getTitular());
                assertTrue(conta.isSituacao());
            }
        } else {
            fail("A conta corrente nao foi cadastrada!");
        }

        LancamentoBancarioRequisicao lancRequisicao = new LancamentoBancarioRequisicao();
        lancRequisicao.setObservacao("Deposito na conta corrente do Albert Einstein");
        lancRequisicao.setValor(new BigDecimal(1234.56));
        lancRequisicao.setData(Formatadores.formatoDataInterface.format(new java.util.Date()));
        lancRequisicao.setIdTipoLancamento(TipoLancamentoEnum.DEPOSITO.getId());
        lancRequisicao.setIdContaCorrente(contas.get(0).getId());
        eventosGestaoContas.salvarLacamentoBancario(lancRequisicao);

        List<Lancamento> lancNovo = gestaoContaBean.pesquisarLancamentoBancarioPorObservacao("Albert");

        if (!lancNovo.isEmpty()) {
            for (Lancamento lancamentoConsultado : lancNovo) {
                assertEquals(lancRequisicao.getObservacao(), lancamentoConsultado.getObservacao());
                assertEquals(lancRequisicao.getValor().doubleValue(), lancamentoConsultado.getValor().doubleValue());
                assertEquals(lancRequisicao.getData(), Formatadores.formatoDataInterface.format(lancamentoConsultado.getData()));
                assertEquals(TipoLancamentoEnum.getByCodigo(lancRequisicao.getIdTipoLancamento()), lancamentoConsultado.getTipoLancamento());
            }
        } else {
            fail("O lancamento bancario nao foi salvo!");
        }

        Calendar novaData = Calendar.getInstance();
        novaData.setTime(Formatadores.formatoDataInterface.parse(lancRequisicao.getData()));
        novaData.add(Calendar.DAY_OF_MONTH, 2);

        LancamentoBancarioAtualizacaoRequisicao atualizarLancamentoRequisicao = new LancamentoBancarioAtualizacaoRequisicao();
        atualizarLancamentoRequisicao.setObservacaoAtualizada("Transferencia para a conta corrente do Charles Darwin");
        atualizarLancamentoRequisicao.setDataAtualizada(Formatadores.formatoDataInterface.format(novaData.getTime()));
        atualizarLancamentoRequisicao.setIdContaCorrente(contas.get(0).getId());
        eventosGestaoContas.atualizarLancamentoBancario(atualizarLancamentoRequisicao);

        List<Lancamento> lancamentoAtualizado = gestaoContaBean.pesquisarLancamentoBancarioPorObservacao("Charles");

        if (!lancamentoAtualizado.isEmpty()) {
            for (Lancamento lancAtualizado : lancamentoAtualizado) {
                assertEquals(atualizarLancamentoRequisicao.getObservacaoAtualizada(), lancAtualizado.getObservacao());
                assertEquals(atualizarLancamentoRequisicao.getDataAtualizada(), Formatadores.formatoDataInterface.format(lancAtualizado.getData()));
            }
        } else {
            fail("O lancamento bancario nao foi atualizado!");
        }
    }

    /** <H3>Teste de Remocao de um Lancamento Bancario cadastrado</H3>
     * <br>
     * <br>
     * <p>
     * Objetivo do teste: Garantir que o sistema esteja excluindo os dados de um
     * lancamento bancario corretamente.</p>
     * <br>
     * <p>
     * <b>Configuração inicial para a realização dos testes: </b> <br>
     * Foi criado um lancamento bancario.</p>
     * <br>
     * <p>
     * <b>Relação de cenários com sua descrição, os passos executados e os
     * resultados esperados. *</b> </p>
     * <ul>
     * <li> <i> Cenário 1: Excluir um lancamento bancario. <i><br>
     * Resultado esperado: Sistema Excluiu os dados do lancamento bancario com
     * sucesso<br>
     * </ul>
     * <br>
     * <p>
     * @since 1.0
     * @author Tadeu
     * @version 2.0 </p>
     */
    @Test
    public void testExcluirLancamentoBancario() throws Exception {
        CadastroContaCorrenteRequisicao cc = new CadastroContaCorrenteRequisicao();
        cc.setAgencia(AgenciaEnum.ARARAS.getId());
        cc.setBanco(BancoEnum.BRADESCO.getId());
        cc.setTitular("Son Gohan");
        eventosGestaoContas.salvarContaCorrente(cc);

        List<ContaCorrente> contas = contaCorrenteDao.pesquisarTodasContasCorrentes();

        if (!contas.isEmpty()) {
            for (ContaCorrente conta : contas) {
                assertTrue(BigDecimal.ZERO.compareTo(conta.getSaldo()) == 0);
                assertEquals(cc.getAgencia(), conta.getAgencia().getId());
                assertEquals(cc.getBanco(), conta.getBanco().getId());
                assertEquals(cc.getTitular(), conta.getTitular());
                assertTrue(conta.isSituacao());
            }
        } else {
            fail("A conta corrente nao foi cadastrada!");
        }

        LancamentoBancarioRequisicao lancRequisicao = new LancamentoBancarioRequisicao();
        lancRequisicao.setObservacao("Deposito na conta corrente do Albert Einstein");
        lancRequisicao.setValor(new BigDecimal(1234.56));
        lancRequisicao.setData(Formatadores.formatoDataInterface.format(new java.util.Date()));
        lancRequisicao.setIdTipoLancamento(TipoLancamentoEnum.DEPOSITO.getId());
        lancRequisicao.setIdContaCorrente(contas.get(0).getId());
        eventosGestaoContas.salvarLacamentoBancario(lancRequisicao);

        List<Lancamento> lancNovo = gestaoContaBean.pesquisarLancamentoBancarioPorObservacao("Albert");

        if (!lancNovo.isEmpty()) {
            for (Lancamento lancamentoConsultado : lancNovo) {
                assertEquals(lancRequisicao.getObservacao(), lancamentoConsultado.getObservacao());
                assertEquals(lancRequisicao.getValor().doubleValue(), lancamentoConsultado.getValor().doubleValue());
                assertEquals(lancRequisicao.getData(), Formatadores.formatoDataInterface.format(lancamentoConsultado.getData()));
                assertEquals(TipoLancamentoEnum.getByCodigo(lancRequisicao.getIdTipoLancamento()), lancamentoConsultado.getTipoLancamento());
            }
        } else {
            fail("O lancamento bancario nao foi salvo!");
        }

        eventosGestaoContas.excluirLancamentoBancario(lancNovo.get(0).getId());

        List<Lancamento> lancExcluido = gestaoContaBean.pesquisarLancamentoBancarioPorObservacao("Albert");

        if (!lancExcluido.isEmpty()) {
            fail("O lancamento bancario nao foi excluido!");
        }
    }

    /** <H3>Teste de Consulta de um Lancamento Bancario cadastrado atraves do
     * tipo do lancamento</H3>
     * <br>
     * <br>
     * <p>
     * Objetivo do teste: Garantir que o sistema esteja excluindo os dados de um
     * lancamento bancario corretamente.</p>
     * <br>
     * <p>
     * <b>Configuração inicial para a realização dos testes: </b> <br>
     * Foi incluido um lancamento na base de dados com o tipo de lancamento
     * SAQUE.</p>
     * <br>
     * <p>
     * <b>Relação de cenários com sua descrição, os passos executados e os
     * resultados esperados. *</b> </p>
     * <ul>
     * <li> <i> Cenário 1: Consultar um lancamento bancario atraves do tipo.
     * <i><br>
     * Resultado esperado: Sistema consultou corretamente o registro atraves do
     * tipo de lancamento informado (SAQUE)<br>
     * </ul>
     * <br>
     * <p>
     * @since 1.0
     * @author Tadeu
     * @version 2.0 </p>
     */
    @Test
    public void testPesquisarLancamentoBancarioPorTipoDeLancamento() throws Exception {
        CadastroContaCorrenteRequisicao cc = new CadastroContaCorrenteRequisicao();
        cc.setAgencia(AgenciaEnum.ARARAS.getId());
        cc.setBanco(BancoEnum.BRADESCO.getId());
        cc.setTitular("Son Gohan");
        eventosGestaoContas.salvarContaCorrente(cc);

        List<ContaCorrente> contas = contaCorrenteDao.pesquisarTodasContasCorrentes();

        if (!contas.isEmpty()) {
            for (ContaCorrente conta : contas) {
                assertTrue(BigDecimal.ZERO.compareTo(conta.getSaldo()) == 0);
                assertEquals(cc.getAgencia(), conta.getAgencia().getId());
                assertEquals(cc.getBanco(), conta.getBanco().getId());
                assertEquals(cc.getTitular(), conta.getTitular());
                assertTrue(conta.isSituacao());
            }
        } else {
            fail("A conta corrente nao foi cadastrada!");
        }

        LancamentoBancarioRequisicao lancRequisicao = new LancamentoBancarioRequisicao();
        lancRequisicao.setObservacao("Saque realizado da conta corrente de Albert Einstein");
        lancRequisicao.setValor(new BigDecimal(1234.56));
        lancRequisicao.setData(Formatadores.formatoDataInterface.format(new java.util.Date()));
        lancRequisicao.setIdTipoLancamento(TipoLancamentoEnum.SAQUE.getId());
        lancRequisicao.setIdContaCorrente(contas.get(0).getId());
        eventosGestaoContas.salvarLacamentoBancario(lancRequisicao);

        List<Lancamento> lancamentoDeSaque = gestaoContaBean.pesquisarLancamentoBancarioPorTipoDeLancamento(TipoLancamentoEnum.SAQUE.getId());

        if (!lancamentoDeSaque.isEmpty()) {
            for (Lancamento lancamentoConsultado : lancamentoDeSaque) {
                assertEquals(lancRequisicao.getObservacao(), lancamentoConsultado.getObservacao());
                assertEquals(lancRequisicao.getValor().doubleValue(), lancamentoConsultado.getValor().doubleValue());
                assertEquals(lancRequisicao.getData(), Formatadores.formatoDataInterface.format(lancamentoConsultado.getData()));
                assertEquals(TipoLancamentoEnum.getByCodigo(lancRequisicao.getIdTipoLancamento()), lancamentoConsultado.getTipoLancamento());
            }
        } else {
            fail("O lancamento bancario nao foi encontrado!");
        }
    }

    /** <H3>Teste de Consulta de um Lancamento Bancario cadastrado atraves de um
     * determinado periodo</H3>
     * <br>
     * <br>
     * <p>
     * Objetivo do teste: Garantir que o sistema esteja excluindo os dados de um
     * lancamento bancario corretamente.</p>
     * <br>
     * <p>
     * <b>Configuração inicial para a realização dos testes: </b> <br>
     * Foram inseridos na base de dados dois lancamentos distintos.</p>
     * <br>
     * <p>
     * <b>Relação de cenários com sua descrição, os passos executados e os
     * resultados esperados. *</b> </p>
     * <ul>
     * <li> <i> Cenário 1: Consultar um lancamento bancario atraves do periodo.
     * <i><br>
     * Resultado esperado: Sistema consultou os lancamentos corretamente atraves
     * do serviço de consulta por periodo.
     * <br>
     * <p>
     * @since 1.0
     * @author Tadeu
     * @version 2.0 </p>
     */
    @Test
    public void testPesquisarLancamentoBancarioPorPeriodo() throws Exception {
        CadastroContaCorrenteRequisicao cc = new CadastroContaCorrenteRequisicao();
        cc.setAgencia(AgenciaEnum.ARARAS.getId());
        cc.setBanco(BancoEnum.BRADESCO.getId());
        cc.setTitular("Son Gohan");
        eventosGestaoContas.salvarContaCorrente(cc);

        List<ContaCorrente> contas = contaCorrenteDao.pesquisarTodasContasCorrentes();

        if (!contas.isEmpty()) {
            for (ContaCorrente conta : contas) {
                assertTrue(BigDecimal.ZERO.compareTo(conta.getSaldo()) == 0);
                assertEquals(cc.getAgencia(), conta.getAgencia().getId());
                assertEquals(cc.getBanco(), conta.getBanco().getId());
                assertEquals(cc.getTitular(), conta.getTitular());
                assertTrue(conta.isSituacao());
            }
        } else {
            fail("A conta corrente nao foi cadastrada!");
        }

        LancamentoBancarioRequisicao lancRequisicao = new LancamentoBancarioRequisicao();
        lancRequisicao.setObservacao("Deposito na conta corrente do Albert Einstein");
        lancRequisicao.setValor(new BigDecimal(1234.56));
        lancRequisicao.setData(Formatadores.formatoDataInterface.format(new java.util.Date()));
        lancRequisicao.setIdTipoLancamento(TipoLancamentoEnum.DEPOSITO.getId());
        lancRequisicao.setIdContaCorrente(contas.get(0).getId());
        eventosGestaoContas.salvarLacamentoBancario(lancRequisicao);

        Calendar novaData = Calendar.getInstance();
        novaData.setTime(Formatadores.formatoDataInterface.parse(lancRequisicao.getData()));
        novaData.add(Calendar.DAY_OF_MONTH, 5);

        LancamentoBancarioRequisicao lancDoisRequisicao = new LancamentoBancarioRequisicao();
        lancDoisRequisicao.setObservacao("Saque realizado na conta corrente de Charles Darwin");
        lancDoisRequisicao.setValor(new BigDecimal(4242.31));
        lancDoisRequisicao.setData(Formatadores.formatoDataInterface.format(novaData.getTime()));
        lancDoisRequisicao.setIdTipoLancamento(TipoLancamentoEnum.SAQUE.getId());
        eventosGestaoContas.salvarLacamentoBancario(lancDoisRequisicao);

        novaData.add(Calendar.DAY_OF_MONTH, 10);
        List<Lancamento> lancamentoDeSaque = gestaoContaBean.pesquisarLancamentoBancarioPorPeriodo(Formatadores.formatoDataInterface.format(new java.util.Date()), Formatadores.formatoDataInterface.format(novaData.getTime().getTime()));

        if (!lancamentoDeSaque.isEmpty()) {
            for (Lancamento lancamentoConsultado : lancamentoDeSaque) {
                if (lancamentoConsultado.getObservacao().equals(lancRequisicao.getObservacao())) {
                    assertEquals(lancRequisicao.getObservacao(), lancamentoConsultado.getObservacao());
                    assertEquals(lancRequisicao.getValor().doubleValue(), lancamentoConsultado.getValor().doubleValue());
                    assertEquals(lancRequisicao.getData(), Formatadores.formatoDataInterface.format(lancamentoConsultado.getData()));
                    assertEquals(TipoLancamentoEnum.getByCodigo(lancRequisicao.getIdTipoLancamento()), lancamentoConsultado.getTipoLancamento());
                } else if (lancamentoConsultado.getObservacao().equals(lancDoisRequisicao.getObservacao())) {
                    assertEquals(lancDoisRequisicao.getObservacao(), lancamentoConsultado.getObservacao());
                    assertEquals(lancDoisRequisicao.getValor().doubleValue(), lancamentoConsultado.getValor().doubleValue());
                    assertEquals(lancDoisRequisicao.getData(), Formatadores.formatoDataInterface.format(lancamentoConsultado.getData()));
                    assertEquals(TipoLancamentoEnum.getByCodigo(lancDoisRequisicao.getIdTipoLancamento()), lancamentoConsultado.getTipoLancamento());
                } else {
                    fail("O lancamento bancario nao foi encontrado!");
                }
            }
        } else {
            fail("O lancamento bancario nao foi encontrado!");
        }
    }
}
