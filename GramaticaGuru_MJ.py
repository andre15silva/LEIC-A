from parte1 import e_palavra
from itertools import permutations

alfabeto = ('A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z')

def cria_palavra_potencial(cad_car, tuplo):
	## SIMPLIFICAR
	if not isinstance(cad_car, str) or not isinstance(tuplo, tuple):
		raise ValueError('cria_palavra_potencial:argumentos invalidos.')
	elif len(cad_car) > len(tuplo):
		raise ValueError('cria_palavra_potencial:a palavra nao e valida.')
	else:
		for e in tuplo:
			if e not in alfabeto:
				raise ValueError('cria_palavra_potencial:argumentos invalidos.')
		for e in cad_car:
			if e not in alfabeto:
				raise ValueError('cria_palavra_potencial:argumentos invalidos.')

		lst_letras = list(tuplo)
		for letra in cad_car:
			if letra in lst_letras:
				lst_letras.remove(letra)
			else:
				raise ValueError('cria_palavra_potencial:a palavra nao e valida.')

		return cad_car

def palavra_tamanho(palavra_pot):
	return len(palavra_pot)

def e_palavra_potencial(arg):
	return isinstance(arg, str) and all(l.isupper() for l in arg)

def palavras_potenciais_iguais(palavra_pot_1, palavra_pot_2):
	return palavra_pot_1 == palavra_pot_2

def palavra_potencial_menor(palavra_pot_1, palavra_pot_2):
	return palavra_pot_1 < palavra_pot_2

def palavra_potencial_para_cadeia(palavra_pot):
	return palavra_pot






def cria_conjunto_palavras():
	return []

def numero_palavras(conj_palavras):
	return len(conj_palavras)

def subconjunto_por_tamanho(conj_palavras, n):
	lst = []
	for palavra_pot in conj_palavras:
		if palavra_tamanho(palavra_pot) == n:
			lst += [palavra_pot]

	return lst

def acrescenta_palavra(conj_palavras, palavra_pot):
	if not e_conjunto_palavras(conj_palavras) or not e_palavra_potencial(palavra_pot):
		raise ValueError('acrescenta_palavra:argumentos invalidos.')

	if palavra_pot not in conj_palavras:
		conj_palavras.append(palavra_pot)

def e_conjunto_palavras(arg):
	return isinstance(arg, list) and all(e_palavra_potencial(e) for e in arg)

def conjuntos_palavras_iguais(conj_palavras_1, conj_palavras_2):
	return sorted(conj_palavras_1, key = lambda p: palavra_potencial_para_cadeia(p)) == \
			sorted(conj_palavras_2, key =  lambda p: palavra_potencial_para_cadeia(p))

def conjunto_palavras_para_cadeia(conj_palavras):
	dic_palavras = {}
	cad_car = ''

	for palavra_pot in conj_palavras:
		tamanho = palavra_tamanho(palavra_pot)
		if tamanho in dic_palavras:
			dic_palavras[tamanho] += [palavra_potencial_para_cadeia(palavra_pot)]
		else:
			dic_palavras[tamanho] = [palavra_potencial_para_cadeia(palavra_pot)]

	for i in sorted(dic_palavras):
		cad_car += str(i) + "->["
		for p in sorted(dic_palavras[i]):
			cad_car += p + ", "
		cad_car = cad_car[:-2] + "];"

	return '[' + cad_car[:-1] + "]"



def cria_jogador(cad_car):
	if not isinstance(cad_car, str):
		raise ValueError('cria_jogador:argumento invalido.')

	return [cad_car,0, cria_conjunto_palavras(), cria_conjunto_palavras()]

def jogador_nome(jogador):
	return jogador[0]

def jogador_pontuacao(jogador):
	return jogador[1]

def jogador_palavras_validas(jogador):
	return jogador[2]

def jogador_palavras_invalidas(jogador):
	return jogador[3]

def adiciona_palavra_valida(jogador, palavra_pot):
	if not e_jogador(jogador) or not e_palavra_potencial(palavra_pot):
		raise ValueError('adiciona_palavra_valida:argumentos invalidos.')
	elif palavra_pot not in jogador_palavras_validas(jogador):
		jogador[1] += palavra_tamanho(palavra_pot)
		acrescenta_palavra(jogador_palavras_validas(jogador), palavra_pot)

def adiciona_palavra_invalida(jogador, palavra_pot):
	if not e_jogador(jogador) or not e_palavra_potencial(palavra_pot):
		raise ValueError('adiciona_palavra_invalida:argumentos invalidos.')
	elif palavra_pot not in jogador_palavras_invalidas(jogador):
		jogador[1] -= palavra_tamanho(palavra_pot)
		acrescenta_palavra(jogador_palavras_invalidas(jogador),palavra_pot)

def e_jogador(jogador):
	return isinstance(jogador, list) \
	       and len(jogador) == 4 \
	       and isinstance(jogador[0], str) \
	       and isinstance(jogador[1], int) \
	       and e_conjunto_palavras(jogador[2]) \
	       and e_conjunto_palavras(jogador[3])

def jogador_para_cadeia(jogador):
	cad_car = 'JOGADOR ' + jogador_nome(jogador) + ' PONTOS=' + str(jogador_pontuacao(jogador)) \
			+ ' VALIDAS=' + conjunto_palavras_para_cadeia(jogador_palavras_validas(jogador)) \
			+ ' INVALIDAS=' + conjunto_palavras_para_cadeia(jogador_palavras_invalidas(jogador))

	return cad_car





def gera_todas_palavras_validas(letras):
	conj = cria_conjunto_palavras()
	lst = []
	for tamanho in range(1, len(letras)+1):
		lst += [''.join(i) for i in permutations(letras, tamanho)]
	for e in lst:
		if e_palavra(e):
			acrescenta_palavra(conj, cria_palavra_potencial(e, letras))

	return conj




def guru_mj(letras):
	print("Descubra todas as palavras geradas a partir das letras:")
	print(letras)

	palavras_por_descobrir = gera_todas_palavras_validas(letras)
	jogadores = []

	print('Introduza o nome dos jogadores (-1 para terminar)...')
	escrita = None
	n_jog = 1
	while escrita != "-1":
		escrita = input('JOGADOR ' + str(n_jog) + ' -> ')
		if escrita != "-1":
			jogadores.append(cria_jogador(escrita))
		n_jog += 1


	n_jogada = 1
	i_jogador_atual = 0
	n_palavras_por_descobrir = numero_palavras(palavras_por_descobrir)
	palavras_descobertas = []

	while n_palavras_por_descobrir > 0:
		jogador_atual = jogadores[i_jogador_atual]
		print("JOGADA " + str(n_jogada) + " - Falta descobrir " + str(n_palavras_por_descobrir) + " palavras")
		jogada = cria_palavra_potencial(input("JOGADOR " + jogador_nome(jogador_atual) + " -> "), letras)

		if jogada in subconjunto_por_tamanho(palavras_por_descobrir, palavra_tamanho(jogada)):
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


		if i_jogador_atual < len(jogadores)-1:
			i_jogador_atual += 1
		else:
			i_jogador_atual = 0

		n_jogada += 1

	vencedores = []
	for j in jogadores:
		if vencedores == []:
			vencedores.append(j)
		elif jogador_pontuacao(j) > jogador_pontuacao(vencedores[0]):
			vencedores = [j]
		elif jogador_pontuacao(j) == jogador_pontuacao(vencedores[0]):
			vencedores.append(j)

	if len(vencedores) == 1:
		print("FIM DE JOGO! O jogo terminou com a vitoria do jogador " + jogador_nome(vencedores[0]) + \
				" com " + str(jogador_pontuacao(vencedores[0])) + " pontos.")
	else:
		print("FIM DE JOGO! O jogo terminou em empate.")

	for j in jogadores:
		print(jogador_para_cadeia(j))

#print(gera_todas_palavras_validas(('A','B','C')))
#guru_mj(('A','B','C'))