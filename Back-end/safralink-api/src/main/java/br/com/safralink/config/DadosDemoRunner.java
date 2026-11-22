package br.com.safralink.config;

import br.com.safralink.domain.CategoriaDesconto;
import br.com.safralink.domain.Cerealista;
import br.com.safralink.domain.Desconto;
import br.com.safralink.domain.FormaDesconto;
import br.com.safralink.domain.Perfil;
import br.com.safralink.domain.Preco;
import br.com.safralink.domain.Produto;
import br.com.safralink.domain.Produtor;
import br.com.safralink.repository.CerealistaRepository;
import br.com.safralink.repository.DescontoRepository;
import br.com.safralink.repository.PrecoRepository;
import br.com.safralink.repository.ProdutoRepository;
import br.com.safralink.repository.ProdutorRepository;
import br.com.safralink.repository.UsuarioRepository;
import br.com.safralink.security.UsuarioPrincipal;
import br.com.safralink.service.EntregaService;
import br.com.safralink.service.NotificacaoService;
import br.com.safralink.service.QuantidadeService;
import br.com.safralink.service.RequisicaoService;
import br.com.safralink.util.Tempo;
import br.com.safralink.web.dto.CatalogoDtos.EntregaRequest;
import br.com.safralink.web.dto.RequisicaoDtos.ContraPropostaRequest;
import br.com.safralink.web.dto.RequisicaoDtos.RequisicaoRequest;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "safralink.dados-demo", havingValue = "true")
public class DadosDemoRunner implements ApplicationRunner {
    static final String SENHA_DEMO = "Safra@2026";
    private static final String EMAIL_ANTONIO = "antonio@safralink.dev";

    private static final Logger log = LoggerFactory.getLogger(DadosDemoRunner.class);

    private final UsuarioRepository usuarioRepository;
    private final CerealistaRepository cerealistaRepository;
    private final ProdutorRepository produtorRepository;
    private final ProdutoRepository produtoRepository;
    private final PrecoRepository precoRepository;
    private final DescontoRepository descontoRepository;
    private final QuantidadeService quantidadeService;
    private final EntregaService entregaService;
    private final RequisicaoService requisicaoService;
    private final NotificacaoService notificacoes;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final EntityManager em;

    public DadosDemoRunner(UsuarioRepository usuarioRepository, CerealistaRepository cerealistaRepository,
                           ProdutorRepository produtorRepository, ProdutoRepository produtoRepository,
                           PrecoRepository precoRepository, DescontoRepository descontoRepository,
                           QuantidadeService quantidadeService, EntregaService entregaService,
                           RequisicaoService requisicaoService, NotificacaoService notificacoes,
                           PasswordEncoder passwordEncoder, JdbcTemplate jdbc, EntityManager em) {
        this.usuarioRepository = usuarioRepository;
        this.cerealistaRepository = cerealistaRepository;
        this.produtorRepository = produtorRepository;
        this.produtoRepository = produtoRepository;
        this.precoRepository = precoRepository;
        this.descontoRepository = descontoRepository;
        this.quantidadeService = quantidadeService;
        this.entregaService = entregaService;
        this.requisicaoService = requisicaoService;
        this.notificacoes = notificacoes;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() == 0) {
            carregarBase();
        }
        if (!usuarioRepository.existsByEmailIgnoreCase(EMAIL_ANTONIO)) {
            carregarProdutoresExtras();
        }
    }

    private void carregarBase() {
        log.info("Carregando dados de demonstração...");

        Cerealista cerealista = new Cerealista();
        cerealista.setNome("Mesa de Compras");
        cerealista.setRazaoSocial("Cerealista Demo Ltda");
        cerealista.setCnpj("11222333000181");
        cerealista.setEmail("cerealista@safralink.dev");
        cerealista.setSenhaHash(passwordEncoder.encode(SENHA_DEMO));
        cerealista.setTelefone("(17) 3000-0000");
        cerealista.getEndereco().setCidade("Salto Grande");
        cerealista.getEndereco().setUf("SP");
        cerealista = cerealistaRepository.save(cerealista);

        Produtor joao = produtor("João da Silva", "joao@safralink.dev", "52998224725", "(14) 99100-0001", "Salto Grande", cerealista);
        Produtor maria = produtor("Maria Oliveira", "maria@safralink.dev", "11144477735", "(14) 99100-0002", "Salto Grande", cerealista);
        Produtor carlos = produtor("Carlos Souza", "carlos@safralink.dev", "12345678909", "(14) 99100-0003", "Salto Grande", cerealista);

        historicoDePrecos(cerealista);
        UsuarioPrincipal pCerealista = principalDe(cerealista);

        Map<String, Long> ids = idsDosProdutos();
        quantidadeService.definir(cerealista.getId(), ids.get("Soja Interno"), new BigDecimal("5000"));
        quantidadeService.definir(cerealista.getId(), ids.get("Soja Externo"), new BigDecimal("3000"));
        quantidadeService.definir(cerealista.getId(), ids.get("Milho Interno"), new BigDecimal("8000"));
        quantidadeService.definir(cerealista.getId(), ids.get("Milho Externo"), new BigDecimal("4000"));
        quantidadeService.definir(cerealista.getId(), ids.get("Feijão Interno"), new BigDecimal("1500"));

        desconto("Armazenagem", CategoriaDesconto.ARMAZENAGEM, FormaDesconto.PERCENTUAL, "1.5");
        desconto("Transporte", CategoriaDesconto.TRANSPORTE, FormaDesconto.VALOR_FIXO, "0.80");

        entrega(cerealista, joao, ids.get("Soja Interno"), "800", 12);
        entrega(cerealista, joao, ids.get("Milho Interno"), "1200", 9);
        entrega(cerealista, maria, ids.get("Soja Interno"), "450", 7);
        entrega(cerealista, maria, ids.get("Feijão Interno"), "120", 5);
        entrega(cerealista, carlos, ids.get("Milho Interno"), "900", 4);
        entrega(cerealista, carlos, ids.get("Soja Externo"), "300", 2);

        UsuarioPrincipal pJoao = principalDe(joao);
        UsuarioPrincipal pMaria = principalDe(maria);
        UsuarioPrincipal pCarlos = principalDe(carlos);

        requisicaoService.criar(pJoao, new RequisicaoRequest(ids.get("Soja Interno"), new BigDecimal("200"), null));
        var aprovada = requisicaoService.criar(pMaria,
                new RequisicaoRequest(ids.get("Soja Interno"), new BigDecimal("100"), null));
        requisicaoService.aprovar(pCerealista, aprovada.id());
        requisicaoService.processar(pCerealista, aprovada.id());
        var recusada = requisicaoService.criar(pCarlos,
                new RequisicaoRequest(ids.get("Milho Interno"), new BigDecimal("300"), new BigDecimal("70.00")));
        requisicaoService.recusar(pCerealista, recusada.id(), "Preço acima do mercado hoje. Envie nova proposta.");

        log.info("Dados de demonstração carregados (cerealista, 3 produtores, preços, saldos e requisições).");
    }

    private void carregarProdutoresExtras() {
        log.info("Carregando produtores de demonstração adicionais...");
        Cerealista cerealista = cerealistaRepository.findFirstByOrderByIdAsc().orElseThrow();
        UsuarioPrincipal pCerealista = principalDe(cerealista);
        Map<String, Long> ids = idsDosProdutos();

        Produtor antonio = produtor("Antônio Marcos Teixeira", EMAIL_ANTONIO, "39053344705", "(14) 99781-2045",
                "Ourinhos", cerealista);
        Produtor helena = produtor("Helena Cristina Duarte", "helena@safralink.dev", "16899535009", "(14) 99633-4410",
                "Chavantes", cerealista);
        UsuarioPrincipal pAntonio = principalDe(antonio);
        UsuarioPrincipal pHelena = principalDe(helena);

        Long sojaInterno = ids.get("Soja Interno");
        Long sojaExterno = ids.get("Soja Externo");
        Long milhoInterno = ids.get("Milho Interno");
        Long milhoExterno = ids.get("Milho Externo");
        Long feijao = ids.get("Feijão Interno");

        entregaDatada(cerealista, antonio, sojaInterno, "1200", 40, "Colheita - talhão 1, 3 viagens");
        entregaDatada(cerealista, antonio, milhoInterno, "1500", 35, "Safrinha - 4 viagens");
        entregaDatada(cerealista, antonio, sojaInterno, "950", 28, "Colheita - talhão 2");
        entregaDatada(cerealista, antonio, milhoInterno, "1100", 20, null);
        entregaDatada(cerealista, antonio, sojaInterno, "600", 15, "Complemento de carga");
        entregaDatada(cerealista, antonio, feijao, "180", 10, "Feijão da seca");

        entregaDatada(cerealista, helena, sojaExterno, "800", 38, "Lote para exportação");
        entregaDatada(cerealista, helena, milhoExterno, "1400", 30, "Safrinha - 5 viagens");
        entregaDatada(cerealista, helena, feijao, "220", 18, null);
        entregaDatada(cerealista, helena, sojaExterno, "700", 22, "Lote para exportação");
        entregaDatada(cerealista, helena, milhoExterno, "900", 12, null);
        entregaDatada(cerealista, helena, sojaInterno, "500", 6, "Carga avulsa");

        long a1 = venda(pAntonio, sojaInterno, "400", "126.40");
        aprovar(pCerealista, a1, true);
        situar(a1, 21, 9);
        long a2 = venda(pAntonio, milhoInterno, "600", "59.80");
        aprovar(pCerealista, a2, true);
        situar(a2, 14, 10);
        long a3 = venda(pAntonio, sojaInterno, "300", "128.90");
        aprovar(pCerealista, a3, false);
        situar(a3, 5, 15);
        long a4 = venda(pAntonio, milhoInterno, "350", "62.50",
                "Consigo entregar na quinta-feira. Se fechar por R$ 63,20 já separo o caminhão.");
        requisicaoService.enviarContraProposta(pAntonio, a4, new ContraPropostaRequest(new BigDecimal("63.20")));
        situar(a4, 1, 11);
        long a5 = venda(pAntonio, feijao, "100", "252.00");
        requisicaoService.recusar(pCerealista, a5, "Estoque de feijão cheio. Podemos retomar na próxima semana.");
        situar(a5, 3, 16);

        long h1 = venda(pHelena, sojaExterno, "500", "131.20");
        aprovar(pCerealista, h1, true);
        situar(h1, 25, 8);
        long h2 = venda(pHelena, milhoExterno, "900", "63.90");
        aprovar(pCerealista, h2, true);
        situar(h2, 10, 14);
        long h3 = venda(pHelena, sojaInterno, "400", "132.00",
                "Preço acima do dia porque o lote é de soja classificada, umidade abaixo de 13%.");
        situar(h3, 0, 8);
        long h4 = venda(pHelena, feijao, "150", "246.00");
        aprovar(pCerealista, h4, false);
        situar(h4, 2, 13);
        long h5 = venda(pHelena, milhoExterno, "450", "66.50");
        requisicaoService.enviarContraProposta(pCerealista, h5, new ContraPropostaRequest(new BigDecimal("64.80")));
        situar(h5, 1, 17);

        em.flush();
        jdbc.update("update notificacoes set lida = true");
        em.clear();
        notificacoes.notificarCerealistas(br.com.safralink.domain.TipoNotificacao.REQUISICAO_CRIADA,
                "Helena Cristina Duarte solicitou a venda de 400 sc de Soja Interno");
        notificacoes.notificarCerealistas(br.com.safralink.domain.TipoNotificacao.CONTRA_PROPOSTA,
                "Antônio Marcos Teixeira propôs R$ 63,20/sc para 350 sc de Milho Interno");

        log.info("Produtores de demonstração adicionais carregados (Antônio e Helena).");
    }

    private long venda(UsuarioPrincipal produtor, Long produtoId, String sacas, String preco) {
        return venda(produtor, produtoId, sacas, preco, null);
    }

    private long venda(UsuarioPrincipal produtor, Long produtoId, String sacas, String preco, String observacao) {
        return requisicaoService.criar(produtor,
                new RequisicaoRequest(produtoId, new BigDecimal(sacas), new BigDecimal(preco), observacao)).id();
    }

    private void aprovar(UsuarioPrincipal cerealista, long requisicaoId, boolean processar) {
        requisicaoService.aprovar(cerealista, requisicaoId);
        if (processar) {
            requisicaoService.processar(cerealista, requisicaoId);
        }
    }

    private void situar(long requisicaoId, int diasAtras, int hora) {
        em.flush();
        Instant criada = Tempo.hoje().minusDays(diasAtras).atTime(hora, 12).atZone(Tempo.ZONA).toInstant();
        Instant decisao = criada.plus(Duration.ofHours(3));
        Instant fim = criada.plus(Duration.ofHours(6));
        Instant agora = Instant.now();
        decisao = decisao.isAfter(agora) ? agora : decisao;
        fim = fim.isAfter(agora) ? agora : fim;
        criada = criada.isAfter(agora) ? agora.minus(Duration.ofMinutes(20)) : criada;

        jdbc.update("update requisicoes_venda set criado_em = ?, atualizado_em = ? where id = ?",
                Timestamp.from(criada), Timestamp.from(decisao), requisicaoId);
        jdbc.update("update transacoes set criado_em = ?, "
                        + "data_processamento = case when data_processamento is null then null else ? end "
                        + "where requisicao_id = ?", Timestamp.from(decisao), Timestamp.from(fim), requisicaoId);
        jdbc.update("update contra_propostas set criado_em = ?, "
                        + "respondida_em = case when respondida_em is null then null else ? end "
                        + "where requisicao_id = ?", Timestamp.from(decisao), Timestamp.from(fim), requisicaoId);
        jdbc.update("update auditorias set criado_em = ? where entidade = 'requisicoes_venda' and entidade_id = ?",
                Timestamp.from(decisao), requisicaoId);
        em.clear();
    }

    private void entregaDatada(Cerealista cerealista, Produtor produtor, Long produtoId, String sacas, int diasAtras,
                               String observacao) {
        var registrada = entregaService.registrar(cerealista.getId(), new EntregaRequest(produtor.getId(), produtoId,
                new BigDecimal(sacas), Tempo.hoje().minusDays(diasAtras), observacao));
        em.flush();
        Instant quando = Tempo.hoje().minusDays(diasAtras).atTime(LocalTime.of(10, 30)).atZone(Tempo.ZONA).toInstant();
        jdbc.update("update entregas set criado_em = ? where id = ?", Timestamp.from(quando), registrada.id());
        jdbc.update("update auditorias set criado_em = ? where entidade = 'entregas' and entidade_id = ?",
                Timestamp.from(quando), registrada.id());
        em.clear();
    }

    private UsuarioPrincipal principalDe(br.com.safralink.domain.Usuario usuario) {
        return new UsuarioPrincipal(usuario);
    }

    private Map<String, Long> idsDosProdutos() {
        Map<String, Long> ids = new HashMap<>();
        produtoRepository.findAll().forEach(p -> ids.put(p.getNome(), p.getId()));
        return ids;
    }

    private Produtor produtor(String nome, String email, String cpf, String telefone, String cidade,
                              Cerealista cerealista) {
        Produtor p = new Produtor();
        p.setNome(nome);
        p.setEmail(email);
        p.setCpf(cpf);
        p.setTelefone(telefone);
        p.setSenhaHash(passwordEncoder.encode(SENHA_DEMO));
        p.getEndereco().setCidade(cidade);
        p.getEndereco().setUf("SP");
        p.setCerealista(cerealista);
        return produtorRepository.save(p);
    }

    private void desconto(String nome, CategoriaDesconto categoria, FormaDesconto forma, String valor) {
        Desconto d = new Desconto();
        d.setNome(nome);
        d.setCategoria(categoria);
        d.setForma(forma);
        d.setValor(new BigDecimal(valor));
        descontoRepository.save(d);
    }

    private void entrega(Cerealista cerealista, Produtor produtor, Long produtoId, String sacas, int diasAtras) {
        entregaService.registrar(cerealista.getId(), new EntregaRequest(produtor.getId(), produtoId,
                new BigDecimal(sacas), Tempo.hoje().minusDays(diasAtras), "Carga de demonstração"));
    }

    private void historicoDePrecos(Cerealista cerealista) {
        Map<String, Double> base = Map.of("Soja Interno", 128.50, "Soja Externo", 134.00, "Milho Interno", 62.40,
                "Milho Externo", 66.00, "Feijão Interno", 245.00);
        List<Preco> precos = new ArrayList<>();
        for (Produto produto : produtoRepository.findByAtivoTrueOrderByIdAsc()) {
            double referencia = base.get(produto.getNome());
            double fase = produto.getId() * 0.9;
            for (int i = 0; i < 30; i++) {
                double fator = 1 + 0.025 * Math.sin(i / 3.5 + fase) + 0.01 * Math.cos(i / 1.7 + fase);
                Preco preco = new Preco();
                preco.setProduto(produto);
                preco.setDefinidoPor(cerealista);
                preco.setValorSaca(BigDecimal.valueOf(referencia * fator).setScale(2, RoundingMode.HALF_UP));
                Instant quando = Tempo.hoje().minusDays(29 - i).atTime(LocalTime.of(8, 0))
                        .atZone(Tempo.ZONA).toInstant();
                preco.setCriadoEm(quando);
                precos.add(preco);
            }
        }
        precoRepository.saveAll(precos);
    }
}
