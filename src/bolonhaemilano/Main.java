package bolonhaemilano;

/*
 * MAIN: o ponto de entrada do jogo.
 *
 * O Java começa a execução sempre pelo método main. Aqui ele só cria o
 * Menu e manda executar. Main não guarda nenhum dado do jogo: quem cuida
 * de tudo é o Menu.
 */
public class Main {

    public static void main(String[] args) {
        Menu menu = new Menu();
        menu.executar();
    }
}
