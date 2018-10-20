% Andre Silva 89408


% acumula_term/3
%   Recebe um termometro e uma posicao e acumula as posicoes ate Pos no resultado.

acumula_term([Pos|_],Pos, [Pos]).

acumula_term([P|RTerm], Pos, [P|RPos]) :-
    acumula_term(RTerm, Pos, RPos).


% propaga/3
%   Recebe um puzzle e uma posicao e afirma que o preenchimento de Pos implica
%   o preenchimento de todas as posicoes na lista Posicoes.

propaga([[PTerm|_]|_], Pos, Posicoes) :-
    member(Pos, PTerm),
    acumula_term(PTerm, Pos, Posicoes_aux),
    sort(Posicoes_aux, Posicoes).

propaga([[_|RTerm]|_], Pos, Posicoes) :-
    propaga([RTerm], Pos, Posicoes).


% nao_altera_linhas_anteriores/3
%   Recebe uma lista de posicoes e uma linha e afirma que dada a lista de posicoes
%   Posicoes, representando uma possibilidade de preenchimento para a linha L,
%   todas as posicoes desta lista cuja linha e' anterior a L ja se encontra
%   preenchida.

nao_altera_linhas_anteriores([], _, _).

nao_altera_linhas_anteriores([(X,_)|RPos], L, Ja_Preenchidas) :-
    X >= L,
    nao_altera_linhas_anteriores(RPos, L, Ja_Preenchidas).

nao_altera_linhas_anteriores([(X,Y)|RPos], L, Ja_Preenchidas) :-
    X < L,
    member((X,Y), Ja_Preenchidas),
    nao_altera_linhas_anteriores(RPos, L, Ja_Preenchidas).


% ocorrencias_coluna/3
%   Afirma que dada uma lista de posicoes e uma coluna C, o numero de posicoes
%   posicoes pertencentes a essa coluna C e' N.

ocorrencias_coluna([], _, 0).

ocorrencias_coluna([(_,C)|RPos], C, N) :-
    ocorrencias_coluna(RPos, C, N_aux),
    N is N_aux + 1.

ocorrencias_coluna([(_,C1)|RPos], C, N) :-
    C1 =\= C,
    ocorrencias_coluna(RPos, C, N).


% verifica_parcial/4
%   Recebe um puzzle, uma lista de posicoes preenchidas, a dimensao do puzzle,
%   e uma lista de posicoes que representa uma possibilidade para preencher
%   uma linha.
%   Afirma que caso a possibilidade seja escolhida, nenhuma coluna excede o
%   total de posicoes a preencher nessa coluna.

verifica_parcial(Puz, Ja_Preenchidas, Dim, Poss) :-
    last(Puz, Colunas),
    union(Ja_Preenchidas, Poss, Posicoes),
    verifica_parcial_aux(Colunas, Posicoes, Dim, 1).

verifica_parcial_aux([], _, Dim, Dim_m_1) :-
    Dim_m_1 is Dim + 1.

verifica_parcial_aux([P|RCol], Posicoes, Dim, C) :-
    C =< Dim,
    ocorrencias_coluna(Posicoes, C, Ocorrencias),
    Ocorrencias =< P,
    C_m_1 is C + 1,
    verifica_parcial_aux(RCol, Posicoes, Dim, C_m_1).


% conta_pertencentes/3
%   Recebe uma lista de posicoes e uma linha L, e afirma que Cont e o numero
%   de posicoes pertencentes a L na lista de posicoes.

conta_pertencentes([], _, 0).

conta_pertencentes([(X,_)|RPos], L, Cont) :-
    X =\= L,
    conta_pertencentes(RPos, L, Cont_aux),
    Cont is Cont_aux.

conta_pertencentes([(X,_)|R], L, Cont) :-
    X =:= L,
    conta_pertencentes(R, L, Cont_aux),
    Cont is 1 + Cont_aux.


% ver_linha/2
%   Recebe uma lista de posicoes, todas da mesma Linha, e afirma que L e' a linha
%   a que estas pertencem.

ver_linha([(L,_)|_], L).


% combinacoes/3
%   Recebe um numero N e uma lista de posicoes L.
%   Devolve uma combinacao de L, N a N.

combinacoes(0, _, []) :-
    !.

combinacoes(N, L, [P|R]) :-
    N > 0,
    N_1 is N - 1,
    escolhe(P, L, Resto),
    combinacoes(N_1, Resto, R).


% escolhe/3
%   Dada uma lista de posicoes escolhe um elemento P e devolve a lista de
%   posicoes R que nao contem P.

escolhe(X, [X|L], L).

escolhe(X, [_|L], R) :-
    escolhe(X, L, R).


% propaga_lista/3
%   Recebe um puzzle e uma lista de posicoes.
%   Aplica o predicado progaga a todas as posicoes da lista original e devolve
%   uma lista de posicoes Propagadas contendo as posicoes da lista original
%   e as posicoes resultantes da propagacao.

propaga_lista(_, [], []).

propaga_lista(Puz, [P|RPos], Propagadas) :-
    propaga(Puz, P, Posicoes),
    propaga_lista(Puz, RPos, Propagadas_aux),
    union(Posicoes, Propagadas_aux, Propagadas).


% propaga_todos/3
%   Recebe um puzzle e uma lista de combinacoes.
%   Aplica o predicado propaga_lista a todos as combinacoes e devolve
%   o resultado.

propaga_todos(_, [], []).

propaga_todos(Puz, [P|RComb], [P_Propagado|RPoss]) :-
    propaga_lista(Puz, P, P_Propagado),
    propaga_todos(Puz, RComb, RPoss).


% filtra_preenchidas/3
%   Recebe uma lista de posicoes preenchidas e uma lista L.
%   Devolve a lista de posicoes apenas contendo as posicoes da linha L.

filtra_preenchidas(_, [], []).

filtra_preenchidas(L, [(X,_)|R], Filtradas) :-
    X =\= L,
    filtra_preenchidas(L, R, Filtradas).

filtra_preenchidas(L, [(X,Y)|R], [(X,Y)|RFiltradas]) :-
    X =:= L,
    filtra_preenchidas(L, R, RFiltradas).


% junta_preenchidas/3
%   Recebe uma lista de listas de posicoes e uma lista de posicoes Ja_Preenchidas.
%   Junta a lista de posicoes Ja_Preenchidas a todas as listas pertencentes
%   a lista de listas.

junta_preenchidas([], _, []).

junta_preenchidas([P_1|R_1], Ja_Preenchidas, [P_2|R_2]) :-
    union(P_1, Ja_Preenchidas, P_2),
    junta_preenchidas(R_1, Ja_Preenchidas, R_2).


% filtra_possibilidades/7
%   Recebe um puzzle, a dimensao do puzzle, uma linha L, o total dessa linha,
%   uma lista de posicoes, Ja_Preenchidas e uma lista de possiveis possibilidades
%   para preencher essa linha.
%   Devolve uma lista de possibilidades que contem as possibilidades da lista
%   fornecida como argumento exceto as que nao safisfazem os requisitos para serem
%   possibilidade de preenchimento da linha L.

filtra_possibilidades(_, _, _, _, _, [], []).

filtra_possibilidades(Puz, Dim, Total, L, Ja_Preenchidas, [P|RPoss], Pos) :-
  ( nao_altera_linhas_anteriores(P, L, Ja_Preenchidas),
    verifica_parcial(Puz, Ja_Preenchidas, Dim, P),
    conta_pertencentes(P, L, N),
    N =:= Total
  ->
    filtra_possibilidades(Puz, Dim, Total, L, Ja_Preenchidas, RPoss, Pos_aux),
    sort(P, P_ordenado),
    union([P_ordenado], Pos_aux, Pos)
  ;
    filtra_possibilidades(Puz, Dim, Total, L, Ja_Preenchidas, RPoss, Pos)
  ).


% possibilidades_linha/5
%   Recebe um puzzle, uma lista de posicoes da linha em questao, o total de
%   posicoes a preencher nessa linha, uma lista de posicoes Ja_Preenchidas.
%   Devolve uma lista de listas de posicoes Possibilidades_L, onde cada elemento
%   representa uma possibilidade para preencher a linha em questao.

possibilidades_linha(Puz, Posicoes_Linha, Total, Ja_Preenchidas, Possibilidades_L) :-
    possibilidades_linha_aux(Puz, Posicoes_Linha, Total, Ja_Preenchidas, Pos),
    sort(Pos, Possibilidades_L).

possibilidades_linha_aux(Puz, Posicoes_Linha, Total, Ja_Preenchidas, Pos) :-
    ver_linha(Posicoes_Linha, L),
    nth0(1, Puz, Linhas),
    length(Linhas, Dim),
    setof(X, combinacoes(Total, Posicoes_Linha, X), Combinacoes_L),
    propaga_todos(Puz, Combinacoes_L, Poss_1),
    filtra_preenchidas(L, Ja_Preenchidas, Ja_Preenchidas_filtradas),
    junta_preenchidas(Poss_1, Ja_Preenchidas_filtradas, Poss_2),
    filtra_possibilidades(Puz, Dim, Total, L, Ja_Preenchidas, Poss_2, Pos).


% gera_linha/4
%   Recebe uma linha L, uma coluna C_min, e uma coluna C_max.
%   Devolve uma lista de posicoes correspondentes as posicoes da linha L
%   a partir da coluna C (inclusive e') ate a coluna C_max (inclusive e')

gera_linha(_, C_min, C_max, []) :-
    C_min > C_max.

gera_linha(L, C_min, C_max, [P|RPosicoes]) :-
    C_min =< C_max,
    P = (L, C_min),
    C_min_m_1 is C_min + 1,
    gera_linha(L, C_min_m_1, C_max, RPosicoes).


% resolve/2
%   Recebe um puzzle e devolve uma lista de posicoes que quando preenchidas
%   resolvem o puzzle em questao

resolve(Puz, Solucao) :-
    % Gera as possiblidades da primeira linha e comeca o processo recursivo
    % do resolve_aux
    nth0(1, Puz, Linhas),
    nth0(0, Linhas, Total_1),
    length(Linhas, Dim),
    gera_linha(1, 1, Dim, Posicoes_Linha),
    possibilidades_linha(Puz, Posicoes_Linha, Total_1, [], Poss),
    resolve_aux(Puz, Dim, 1, Poss, [], Solucao_aux),
    sort(Solucao_aux, Solucao).

resolve_aux(_, Dim, Dim, [P|_], Ja_Preenchidas, Solucao_aux) :-
    % Se chegou a ultima linha e existe uma possibilidade entao foi encontrada
    % uma solucao.
    union(P, Ja_Preenchidas, Solucao_aux).

resolve_aux(Puz, Dim, L, [P|_], Ja_Preenchidas, Solucao) :-
    % Tenta resolver com a primeira possibilidade da lista de possibilidades
    L < Dim,
    union(P, Ja_Preenchidas, Poss),
    nth0(1, Puz, Linhas),
    Prox_L is L + 1,
    nth0(L, Linhas, Total_Prox_L),
    gera_linha(Prox_L, 1, Dim, Posicoes_Linha),
    possibilidades_linha(Puz, Posicoes_Linha, Total_Prox_L, Poss, Poss_Prox_L),
    resolve_aux(Puz, Dim, Prox_L, Poss_Prox_L, Poss, Solucao),
    !.

resolve_aux(Puz, Dim, L, [_|RPoss], Ja_Preenchidas, Solucao) :-
    % Tenta com o resto das possibilidades.
    L < Dim,
    resolve_aux(Puz, Dim, L, RPoss, Ja_Preenchidas, Solucao).
