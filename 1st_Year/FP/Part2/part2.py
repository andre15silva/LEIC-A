# 89408 - Andre Afonso Nunes Silva

from parte1 import e_palavra
from itertools import permutations



#----------TAD palavra_potencial----------
# Representacao interna: uma string correspondente a
#   cad_car fornecida como argumento no construtor do tipo


def cria_palavra_potencial(cad_car, tuplo):
    """
    Contrutor do TAD palavra_potencial

    Argumentos:
    cad_car -- string da palavra_potencial
    tuplo -- conjunto de letras
    """
    if isinstance(cad_car, str) and \
        isinstance(tuplo, tuple) and \
        all(l.isupper() for l in cad_car) and \
        all(isinstance(l, str) for l in tuplo) and \
        all(len(l) == 1 for l in tuplo) and \
        all(l.isupper() for l in tuplo):

        if len(cad_car) <= len(tuplo):
            lst_letras = list(tuplo)
            for letra in cad_car:
                if letra in lst_letras:
                    lst_letras.remove(letra)
                else:
                    raise ValueError('cria_palavra_potencial:a palavra nao e valida.')

            return cad_car

        else:
            raise ValueError('cria_palavra_potencial:a palavra nao e valida.')
    else:
        raise ValueError('cria_palavra_potencial:argumentos invalidos.')


def palavra_tamanho(palavra_pot):
    """
    Seletor do TAD palavra_potencial
    Devolve o tamanho da palavra_potencial

    Argumentos:
    palavra_pot -- palavra_potencial
    """
    return len(palavra_pot)


def e_palavra_potencial(arg):
    """
    Reconhecedor do TAD palavra_potencial
    Verifica se o arg e palavra_potencial e devolve valor booleano

    Argumentos:
    arg -- argumento a verificar
    """
    return isinstance(arg, str) and all(l.isupper() for l in arg)


def palavras_potenciais_iguais(palavra_pot_1, palavra_pot_2):
    """
    Teste do TAD palavra_potencial
    Verifica se as palavras_potenciais sao iguais e devolte valor booleano

    Argumentos:
    palavra_pot_1 -- palavra_potencial
    palavra_pot_2 -- palavra_potencial
    """
    return palavra_pot_1 == palavra_pot_2


def palavra_potencial_menor(palavra_pot_1, palavra_pot_2):
    """
    Teste do TAD palavra_potencial
    Verifica se a primeira palavras_potenciais e menor que a segunda e devolve valor booleano

    Argumentos:
    palavra_pot_1 -- palavra_potencial
    palavra_pot_2 -- palavra_potencial
    """
    return palavra_pot_1 < palavra_pot_2


def palavra_potencial_para_cadeia(palavra_pot):
    """
    Transformador do TAD palavra_potencial
    Devolve a representacao da palavra_potencial em string

    Argumentos:
    palavra_pot -- palavra_potencial
    """
    return palavra_pot



#----------TAD conjunto_palavras----------
# Representacao interna: uma lista


def cria_conjunto_palavras():
    """
    Contrutor do TAD conjunto_palavras
    Cria um conjunto_palavras vazio
    """
    return []


def numero_palavras(conj_palavras):
    """
    Seletor do TAD conjunto_palavras
    Devolve o numero de palavras_potencial no conjunto_palavras

    Argumentos:
    conj_palavras -- conjunto_palavras
    """
    return len(conj_palavras)


def subconjunto_por_tamanho(conj_palavras, n):
    """
    Seletor do TAD conjunto_palavras
    Devolve uma lista com as palavras_potencias de n tamanho contidas no conjunto_palavras

    Argumentos:
    conj_palavras -- conjunto_palavras
    n -- inteiro correspondente ao tamanho das palavras_potenciais
    """
    return [p for p in conj_palavras if palavra_tamanho(p) == n]


def acrescenta_palavra(conj_palavras, palavra_pot):
    """
    Modificador do TAD conjunto_palavras
    Acrescenta a palavra_potencial ao conjunto_palavras caso ainda nao pertenca

    Argumentos:
    conj_palavras -- conjunto_palavras
    palavra_pot -- palavra_potencial
    """
    if not e_conjunto_palavras(conj_palavras) or not e_palavra_potencial(palavra_pot):
        raise ValueError('acrescenta_palavra:argumentos invalidos.')

    elif palavra_pot not in conj_palavras:
        conj_palavras.append(palavra_pot)


def e_conjunto_palavras(arg):
    """
    Reconhecedor do TAD conjunto_palavras
    Verifica se o arg e conjunto_palavras e devolve valor booleano

    Argumentos:
    arg -- argumento a verificar
    """
    return isinstance(arg, list) and all(e_palavra_potencial(e) for e in arg)


def conjuntos_palavras_iguais(conj_palavras_1, conj_palavras_2):
    """
    Teste do TAD conjunto_palavras
    Verifica se os conjuntos_palavras sao iguais

    Argumentos:
    conj_palavras_1 -- conjunto_palavras
    conj_palavras_2 -- conjunto_palavras
    """

    def ordena_conjunto(conj_palavras):
        """
        Transformador do TAD conjunto_palavras
        Transforma o conjunto_palavras numa lista com as palavras_potenciais ordenadas

        Argumentos:
        conj_palavras -- conjunto_palavras
        """
        res = conj_palavras[:]

        ordenado = False
        while not ordenado:
            ordenado = True
            for i in range(len(res) - 1):
                if palavra_potencial_menor(res[i + 1], res[i]):
                    ordenado = False
                    res[i], res[i + 1] = res[i + 1], res[i]

        return res

    # Precisamos de ordenar os conjuntos de modo a poder compara-los. Utilizamos um algoritmo
    #   de bubble sort para ordenar de acordo com a ordem das palavras_potenciais
    return ordena_conjunto(conj_palavras_1) == ordena_conjunto(conj_palavras_2)


def conjunto_palavras_para_cadeia(conj_palavras):
    """
    Transformador do tipo conjunto_palavras
    Devolve a representacao do conjunto_palavras em string

    Argumentos:
    conj_palavras -- conjunto_palavras
    """
    dic_palavras = {}
    cad_car = ""

    # Separa o conjunto consoante o tamanho das palavras_potenciais
    for palavra_pot in conj_palavras:
        tamanho = palavra_tamanho(palavra_pot)
        if tamanho in dic_palavras:
            dic_palavras[tamanho] += [palavra_potencial_para_cadeia(palavra_pot)]
        else:
            dic_palavras[tamanho] = [palavra_potencial_para_cadeia(palavra_pot)]

    # Constroi a string a retornar
    for i in sorted(dic_palavras):
        cad_car += str(i) + "->["
        for p in sorted(dic_palavras[i]):
            cad_car += p + ", "
        cad_car = cad_car[:-2] + "];"

    return "[" + cad_car[:-1] + "]"



#----------TAD jogador----------
# Representacao interna:
#   Uma lista onde:
#   - primeira posicao : nome do jogador
#   - segunda posicao : pontuacao do jogador
#   - terceira posicao : conjunto de palavras validas
#   - quarta posicao : conjunto de palavras invalidas


def cria_jogador(cad_car):
    """
    Construtor do TAD jogador
    Cria um jogador com o nome correspondente a cad_car

    Argumentos:
    cad_car -- string
    """
    if not isinstance(cad_car, str):
        raise ValueError('cria_jogador:argumento invalido.')

    return [cad_car, 0, cria_conjunto_palavras(), cria_conjunto_palavras()]


def jogador_nome(jogador):
    """
    Seletor do TAD jogador
    Devolve o valor do nome do jogador

    Argumentos:
    jogador -- jogador
    """
    return jogador[0]


def jogador_pontuacao(jogador):
    """
    Seletor do TAD jogador
    Devolve o valor da pontucao do jogador

    Argumentos:
    jogador -- jogador
    """
    return jogador[1]


def jogador_palavras_validas(jogador):
    """
    Seletor do TAD jogador
    Devolve o conjunto_palavras validas do jogador

    Argumentos:
    jogador -- jogador
    """
    return jogador[2]


def jogador_palavras_invalidas(jogador):
    """
    Seletor do TAD jogador
    Devolve o conjunto_palavras invalidas do jogador

    Argumentos:
    jogador -- jogador
    """
    return jogador[3]


def adiciona_palavra_valida(jogador, palavra_pot):
    """
    Modificador to TAD jogador
    Acrescenta a palavra_potencial ao conjunto_palavras validas do jogador e adiciona os pontos
        correspondentes, caso esta ainda nao pertenca

    Argumentos:
    jogador -- jogador
    palavra_pot -- palavra_potencial
    """
    if not e_jogador(jogador) or not e_palavra_potencial(palavra_pot):
        raise ValueError('adiciona_palavra_valida:argumentos invalidos.')

    elif palavra_pot not in jogador_palavras_validas(jogador):
        jogador[1] += palavra_tamanho(palavra_pot)
        acrescenta_palavra(jogador_palavras_validas(jogador), palavra_pot)


def adiciona_palavra_invalida(jogador, palavra_pot):
    """
    Modificador to TAD jogador
    Acrescenta a palavra_potencial ao conjunto_palavras invalidas do jogador e retira os pontos
        correspondentes, caso esta ainda nao pertenca

    Argumentos:
    jogador -- jogador
    palavra_pot -- palavra_potencial
    """
    if not e_jogador(jogador) or not e_palavra_potencial(palavra_pot):
        raise ValueError('adiciona_palavra_invalida:argumentos invalidos.')

    elif palavra_pot not in jogador_palavras_invalidas(jogador):
        jogador[1] -= palavra_tamanho(palavra_pot)
        acrescenta_palavra(jogador_palavras_invalidas(jogador), palavra_pot)


def e_jogador(jogador):
    """
    Reconhecedor do TAD jogador
    Verifica se o arg e jogador e devolve valor booleano

    Argumentos:
    arg -- argumento a verificar
    """
    return isinstance(jogador, list) \
           and len(jogador) == 4 \
           and isinstance(jogador[0], str) \
           and isinstance(jogador[1], int) \
           and e_conjunto_palavras(jogador[2]) \
           and e_conjunto_palavras(jogador[3])


def jogador_para_cadeia(jogador):
    """
    Transformador do TAD jogador
    Devolve a representacao do jogador em string

    Argumentos:
    jogador -- jogador
    """
    cad_car = 'JOGADOR ' + jogador_nome(jogador) \
            + ' PONTOS=' + str(jogador_pontuacao(jogador)) \
            + ' VALIDAS=' + conjunto_palavras_para_cadeia(jogador_palavras_validas(jogador)) \
            + ' INVALIDAS=' + conjunto_palavras_para_cadeia(jogador_palavras_invalidas(jogador))

    return cad_car


#----------Funcoes adicionais-----------


def gera_todas_palavras_validas(letras):
    """
    Devolve um conjunto_palavras com todas as palavras_validas possiveis de gerar com o conjunto de letras

    Argumentos:
    letras -- tuplo
    """
    conj = cria_conjunto_palavras()
    lst = []
    for tamanho in range(1, len(letras) + 1):
        lst += [''.join(l) for l in permutations(letras, tamanho)]
    for e in lst:
        if e_palavra(e):
            acrescenta_palavra(conj, cria_palavra_potencial(e, letras))

    return conj


def guru_mj(letras):
    """
    Funcao que gera o jogo

    Argumentos:
    letras -- tuplo
    """
    print("Descubra todas as palavras geradas a partir das letras:")
    print(letras)

    palavras_por_descobrir = gera_todas_palavras_validas(letras)
    jogadores = []

    # Criacao dos jogadores a participar no jogo
    print('Introduza o nome dos jogadores (-1 para terminar)...')
    escrita = ""
    n_jog = 1
    while escrita != "-1":
        escrita = input('JOGADOR ' + str(n_jog) + ' -> ')
        if escrita != "-1":
            jogadores.append(cria_jogador(escrita))
        n_jog += 1

    # Introducao das jogadas pelos jogadores
    n_jogada = 1
    i_jogador_atual = 0
    n_palavras_por_descobrir = numero_palavras(palavras_por_descobrir)
    palavras_descobertas = []

    while n_palavras_por_descobrir > 0:
        jogador_atual = jogadores[i_jogador_atual]
        print("JOGADA " + str(n_jogada) + " - Falta descobrir " + str(n_palavras_por_descobrir) + " palavras")
        jogada = cria_palavra_potencial(input("JOGADOR " + jogador_nome(jogador_atual) + " -> "), letras)

        # Verificacao da validade da jogada
        if jogada in subconjunto_por_tamanho(palavras_por_descobrir,palavra_tamanho(jogada)):
            if jogada in palavras_descobertas:
                print(palavra_potencial_para_cadeia(jogada) + " - palavra VALIDA")
            else:
                print(palavra_potencial_para_cadeia(jogada) + " - palavra VALIDA")
                n_palavras_por_descobrir -= 1
                adiciona_palavra_valida(jogador_atual, jogada)
                palavras_descobertas.append(jogada)
        else:
            print(palavra_potencial_para_cadeia(jogada) + " - palavra INVALIDA")
            adiciona_palavra_invalida(jogador_atual, jogada)

        # Ja chegamos ao fim dos jogadores?
        if i_jogador_atual < len(jogadores) - 1:
            i_jogador_atual += 1
        else:
            i_jogador_atual = 0

        n_jogada += 1

    # Determincao do vencedor do jogo
    vencedores = []
    for j in jogadores:
        if vencedores == []:
            vencedores.append(j)
        elif jogador_pontuacao(j) > jogador_pontuacao(vencedores[0]):
            vencedores = [j]
        elif jogador_pontuacao(j) == jogador_pontuacao(vencedores[0]):
            vencedores.append(j)

    # Print da mensagem final consoante o resultado
    if len(vencedores) == 1:
        print("FIM DE JOGO! O jogo terminou com a vitoria do jogador " + jogador_nome(vencedores[0]) + \
                " com " + str(jogador_pontuacao(vencedores[0])) + " pontos.")
    else:
        print("FIM DE JOGO! O jogo terminou em empate.")

    for j in jogadores:
        print(jogador_para_cadeia(j))
