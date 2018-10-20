# 89408 Andre Afonso Nunes Silva
# Verificador de cadeias de caracteres de acordo com a Gramatica Guru
# Projeto desenvolvido na Unidade Curricular de Fundamentos da Programacao


#---------------------------------------------------------------#
#                 Tuplos com simbolos terminais                 #
#---------------------------------------------------------------#
# Declaracao de tuplos com os simbolos terminais de cada regra

ARTIGO_DEF         = ('A','O')
VOGAL_PALAVRA      = ('E',) + ARTIGO_DEF
VOGAL              = ('I','U') + VOGAL_PALAVRA

DITONGO_PALAVRA    = ('AI','AO','EU','OU')
DITONGO            = ('AE','AU','EI','OE','OI','IU') + DITONGO_PALAVRA
PAR_VOGAIS         = ('IA','IO') + DITONGO

CONSOANTE_FREQ     = ('D','L','M','N','P','R','S','T','V')
CONSOANTE_TERMINAL = ('L','M','R','S','X','Z')
CONSOANTE_FINAL    = ('N','P') + CONSOANTE_TERMINAL
CONSOANTE          = ('B','C','D','F','G','H','J','L','M','N','P','Q','R','S','T','V','X','Z')

PAR_CONSOANTES     = ('BR','CR','FR','GR','PR','TR','VR','BL','CL','FL','GL','PL')

SILABA_3           = ('QUA','QUE','QUI','GUE','GUI')

MONOSSILABO_2      = ('AR','IR','EM','UM')


#---------------------------------------------------------------#
#                     Funcoes Principais                        #
#---------------------------------------------------------------#

def e_palavra(cad_caracteres):
    '''
    e_palavra:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <palavra> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        e_monossilabo
        e_silaba_final
        e_palavra_aux (definida dentro da funcao e_palavra)
    '''
    def e_palavra_aux(cad_caracteres):
        '''
        e_palavra_aux:
            Verifica se a cadeia de caracteres recebida pode ser dividida de modo a que
                cada uma das cadeias de caracteres resultantes da divisao estejam em
                concordancia com a regra <silaba> da Gramatica Guru
            Ou seja, verifica a regra <silaba>*

        Argumento:
            Uma cadeia de caracteres
        Retorno:
            Valor Booleano (True ou False)

         Funcoes utilizadas:
            e_silaba
            e_palavra_aux (recursao)
        '''
        if e_silaba(cad_caracteres):                                # Se a cadeia de caracteres depois de todos os cortes for silaba entao a palavra verifica as regras
            return True

        else:
            max = 6
            if len(cad_caracteres) < 6:                             # Otimizacao para nao ter de verificar mais do que o necessario
                max = len(cad_caracteres)

            for i in range(1,max):
                if e_silaba(cad_caracteres[:i]):                    # Verifica as 5 cadeias de caracteres iniciais, com ate 5 letras.

                    if e_palavra_aux(cad_caracteres[i:]):           # Se alguma for volta a verificar, mas agr com essa silaba cortada.
                        return True

            return False

    if not isinstance(cad_caracteres, str):
        raise ValueError('e_palavra:argumento invalido')

    elif e_monossilabo(cad_caracteres):                             # Verifica se a cadeia de caracteres e monossilabo
        return True

    elif e_silaba_final(cad_caracteres):                            # Verifica se a cadeia de caracteres e silaba_final
        return True

    else:
        for i in range(-5, -1):

            if e_silaba_final(cad_caracteres[i:]):                  # Se nao for ve se ha alguma das cadeias de caracteres com ate 5 letras a contar do final e silaba_final

                if e_palavra_aux(cad_caracteres[:i]):               # As que forem verifica as divisoes possiveis de silabas do resto da cadeia de caracteres inicial
                    return True

        return False


def e_silaba(cad_caracteres):
    '''
    e_silaba:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <silaba> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        e_vogal
        e_silaba_2
        e_silaba_3
        e_silaba_4
        e_silaba_5
    '''
    if isinstance(cad_caracteres, str):
        if len(cad_caracteres) == 1:
            return e_vogal(cad_caracteres)

        elif len(cad_caracteres) == 2:
            return e_silaba_2(cad_caracteres)

        elif len(cad_caracteres) == 3:
            return e_silaba_3(cad_caracteres)

        elif len(cad_caracteres) == 4:
            return e_silaba_4(cad_caracteres)

        elif len(cad_caracteres) == 5:
            return e_silaba_5(cad_caracteres)

        else:
            return False
    else:
        raise ValueError('e_silaba:argumento invalido')


def e_monossilabo(cad_caracteres):
    '''
    e_monossilabo:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <monossilabo> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        e_vogal_palavra
        e_monossilabo_2
        e_monossilabo_3
    '''
    if isinstance(cad_caracteres, str):
        if len(cad_caracteres) == 1:
            return e_vogal_palavra(cad_caracteres)

        elif len(cad_caracteres) == 2:
            return e_monossilabo_2(cad_caracteres)

        elif len(cad_caracteres) == 3:
            return e_monossilabo_3(cad_caracteres)

        else:
            return False
    else:
        raise ValueError('e_monossilabo:argumento invalido')


#---------------------------------------------------------------#
#               Funcoes Auxiliares Principais                   #
#---------------------------------------------------------------#

def e_silaba_final(cad_caracteres):
    '''
    e_silaba_final:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <silaba_final> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        e_monossilabo_2
        e_monossilabo_3
        e_silaba_4
        e_silaba_5
    '''
    if len(cad_caracteres) == 2:
        return e_monossilabo_2(cad_caracteres)

    elif len(cad_caracteres) == 3:
        return e_monossilabo_3(cad_caracteres)

    elif len(cad_caracteres) == 4:
        return e_silaba_4(cad_caracteres)

    elif len(cad_caracteres) == 5:
        return e_silaba_5(cad_caracteres)

    else:
        return False


def e_vogal(cad_caracteres):
    '''
    e_vogal:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <vogal> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        ---
    '''
    return cad_caracteres in VOGAL


def e_silaba_2(cad_caracteres):
    '''
    e_silaba_2:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <silaba_2> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        e_par_vogais
        e_consoante
        e_vogal
        e_consoante_final
    '''
    if e_par_vogais(cad_caracteres):
        return True

    elif e_consoante(cad_caracteres[0]) and e_vogal(cad_caracteres[1]):
        return True

    else:
        return e_vogal(cad_caracteres[0]) and e_consoante_final(cad_caracteres[1])


def e_silaba_3(cad_caracteres):
    '''
    e_silaba_3:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <silaba_4> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        e_vogal
        e_consoante
        e_par_vogais
        e_consoante_final
    '''
    if e_vogal(cad_caracteres[0]) and cad_caracteres[1:] == 'NS':
        return True

    elif e_consoante(cad_caracteres[0]) and e_par_vogais(cad_caracteres[1:]):
        return True

    elif e_consoante(cad_caracteres[0]) and e_vogal(cad_caracteres[1]) and e_consoante_final(cad_caracteres[2]):
        return True

    elif e_par_vogais(cad_caracteres[:2]) and e_consoante_final(cad_caracteres[2]):
        return True

    elif e_par_consoantes(cad_caracteres[:2]) and e_vogal(cad_caracteres[2]):
        return True

    else:
        return cad_caracteres in SILABA_3


def e_silaba_4(cad_caracteres):
    '''
    e_silaba_4:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <silaba_4> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        e_par_vogais
        e_consoante
        e_vogal
        e_par_consoantes
        e_consoante_final
    '''
    if e_par_vogais(cad_caracteres[:2]) and cad_caracteres[2:] == 'NS':
        return True

    elif e_consoante(cad_caracteres[0]) and e_vogal(cad_caracteres[1]) and cad_caracteres[2:] == 'NS':
        return True

    elif e_consoante(cad_caracteres[0]) and e_vogal(cad_caracteres[1]) and cad_caracteres[2:] == 'IS':
        return True

    elif e_par_consoantes(cad_caracteres[:2]) and e_par_vogais(cad_caracteres[2:]):
        return True

    else:
        return e_consoante(cad_caracteres[0]) and e_par_vogais(cad_caracteres[1:3]) and e_consoante_final(cad_caracteres[3])


def e_silaba_5(cad_caracteres):
    '''
    e_silaba_5:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <silaba_5> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        e_par_consoantes
        e_vogal
    '''
    return e_par_consoantes(cad_caracteres[:2]) and e_vogal(cad_caracteres[2]) and cad_caracteres[3:] == 'NS'


def e_monossilabo_2(cad_caracteres):
    '''
    e_monossilabo_2:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <monossilabo_2> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        e_vogal_palavra
        e_ditongo_palavra
        e_consoante_freq
        e_vogal
    '''
    if e_vogal_palavra(cad_caracteres[0]) and cad_caracteres[1] == 'S':
        return True

    elif e_ditongo_palavra(cad_caracteres):
        return True

    elif e_consoante_freq(cad_caracteres[0]) and e_vogal(cad_caracteres[1]):
        return True

    else:
        return cad_caracteres in MONOSSILABO_2


def e_monossilabo_3(cad_caracteres):
    '''
    e_monossilabo_3:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <monossilabo_3> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        e_consoante
        e_vogal
        e_consoante_terminal
        e_ditongo
        e_par_vogais
    '''
    if e_consoante(cad_caracteres[0]) and e_vogal(cad_caracteres[1]) and e_consoante_terminal(cad_caracteres[2]):
        return True

    elif e_consoante(cad_caracteres[0]) and e_ditongo(cad_caracteres[1:]):
        return True

    else:
        return e_par_vogais(cad_caracteres[:2]) and e_consoante_terminal(cad_caracteres[2])


#---------------------------------------------------------------#
#                Funcoes Auxiliares Secundarias                 #
#---------------------------------------------------------------#

def e_par_consoantes(cad_caracteres):
    '''
    e_par_consoantes:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <par_consoantes> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        ---
    '''
    return cad_caracteres in PAR_CONSOANTES


def e_consoante(cad_caracteres):
    '''
    e_consoante:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <consoante> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        ---
    '''
    return cad_caracteres in CONSOANTE


def e_consoante_final(cad_caracteres):
    '''
    e_consoante_final:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <consoante_final> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        ---
    '''
    return cad_caracteres in CONSOANTE_FINAL


def e_consoante_terminal(cad_caracteres):
    '''
    e_consoante_terminal:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <consoante_terminal> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        ---
    '''
    return cad_caracteres in CONSOANTE_TERMINAL


def e_consoante_freq(cad_caracteres):
    '''
    e_consoante_freq:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <consoante_freq> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        ---
    '''
    return cad_caracteres in CONSOANTE_FREQ


def e_par_vogais(cad_caracteres):
    '''
    e_par_vogais:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <par_vogais> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        ---
    '''
    return cad_caracteres in PAR_VOGAIS


def e_ditongo(cad_caracteres):
    '''
    e_ditongo:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <ditongo> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        ---
    '''
    return cad_caracteres in DITONGO


def e_ditongo_palavra(cad_caracteres):
    '''
    e_ditongo_palavra::
        Verifica a concordancia da cadeia de caracteres recebida com a regra <ditongo_palavra> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        ---
    '''
    return cad_caracteres in DITONGO_PALAVRA


def e_vogal_palavra(cad_caracteres):
    '''
    e_vogal_palavra:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <vogal_palavra> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        ---
    '''
    return cad_caracteres in VOGAL_PALAVRA


def e_artigo_def(cad_caracteres):
    '''
    e_artigo_def:
        Verifica a concordancia da cadeia de caracteres recebida com a regra <artigo_def> da Gramatica Guru.

    Argumento:
        Uma cadeia de caracteres
    Retorno:
        Valor Booleano (True ou False)

    Funcoes utilizadas:
        ---
    '''
    return cad_caracteres in ARTIGO_DEF