package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=" + System.getenv("OMDB_APIKEY");
    private List<DadosSerie> dadosSeries = new ArrayList<>();
    List<Serie> series = new ArrayList<>();
    private SerieRepository repositorio;


    public Principal(SerieRepository repositorio) {
        this.repositorio = repositorio;
    }


    public void exibeMenu() {

        var menu = """
                
                */*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*
                
                    1 - Buscar séries
                    2 - Buscar episódios
                    3 - Listar séries buscadas
                    4 - Buscar série por título
                    5 - Buscar séries por ator
                    6 - Buscar séries por categoria
                    7 - Top 5 Séries
                    8 - Buscar por numero de Temporadas/Avaliação
                    
                    0 - Sair                                 
                """;
        var opcao = -1;


        while (opcao != 0) {
            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriesPorAtor();
                    break;
                case 6:
                    buscarSeriesPorCategoria();
                    break;
                case 7:
                    buscaTop5Series();
                    break;
                case 8:
                    buscarPorNumeroDeTemporadas();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        repositorio.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();
        System.out.println("Escolha uma serie pelo nome: ");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serie = series.stream()
                .filter(s -> s.getTitulo().toLowerCase().contains(nomeSerie.toLowerCase()))
                .findFirst();

        if (serie.isPresent()) {
            var serieEncontrada = serie.get();
            List<DadosTemporadas> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporadas dadosTemporada = conversor.obterDados(json, DadosTemporadas.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream())
                    .map(e -> new Episodio(e.numero(), e))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
        } else {
            System.out.println("Serie não encontrada!");
        }
    }

    private void  listarSeriesBuscadas() {
        series = repositorio.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);

    }

    private void buscarSeriePorTitulo() {
        System.out.println("Escolha uma serie pelo nome: ");
        var nomeSerie = leitura.nextLine();
        Optional<Serie> serieBuscada = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if ( serieBuscada.isPresent()){
            System.out.println("Dados da serie: " + serieBuscada);
        } else System.out.println("Serie não encontrada");

    }

    private void buscarSeriesPorAtor() {
        System.out.println("Qual o nome do ator para a busca?");
        var nomeAtor = leitura.nextLine();
        System.out.println("Qual a nota minima da serie para a busca?");
        var nota = leitura.nextDouble();
        List<Serie> atorBuscado = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, nota);
        System.out.println("Series em que " + nomeAtor + " trabalhou: ");
        atorBuscado.forEach(s-> System.out.println(s.getTitulo() + ", com uma avaliação de " + s.getAvaliacao() + "!"));
    }

    private void buscaTop5Series() {
        System.out.println("As top 5 series do banco são :");
        List<Serie> top5 = repositorio.findTop5ByOrderByAvaliacaoDesc();
        top5.forEach(t -> System.out.println(t.getTitulo() + ", com uma avalição de " + t.getAvaliacao() + "!"));
    }

    private void buscarSeriesPorCategoria() {
        System.out.println("Deseja buscar séries de que categoria/gênero? ");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);
        System.out.println("Séries da categoria " + nomeGenero + ".");
        seriesPorCategoria.forEach(System.out::println);
    }

    private void buscarPorNumeroDeTemporadas() {
        System.out.println("Você quer ver séries com até quantas temporadas?");
        var temporadas = leitura.nextInt();
        List<Serie> serieList = repositorio.findByTotalTemporadasLessThanEqualAndAvaliacaoGreaterThanEqual(
                temporadas, 8.5);
        serieList.forEach(t -> System.out.println(t.getTitulo() + ", tem um total de " + t.getTotalTemporadas() +
                " temporadas e uma nota de " + t.getAvaliacao() + "!"));

    }


}


