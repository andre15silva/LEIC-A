;MasterMind

;Andre Afonso Nunes Silva - 89408
;Rafael Nunes Henriques   - 89530

;Projeto desenvolvido na Unidade Curricular de Introducao a Arquitetura de Computadores



;----------------------------------------------------
;				CONSTANTES
;----------------------------------------------------

SP_INICIAL				EQU		FDFFh
MASCARA_RANDOM			EQU 	1000000000010110b		;mascara para a funcao random
STR_END 				EQU 	'@' 					;fim das strings
clrscreen 				EQU 	' '						;espaco em branco para clrscr
INT_MASK_ADDR			EQU 	FFFAh
IO_DISPLAY_1			EQU 	FFF0h
IO_DISPLAY_2			EQU 	FFF1h
IO_DISPLAY_3			EQU 	FFF2h
IO_DISPLAY_4			EQU 	FFF3h
LCD_CTRL				EQU 	FFF4h
LCD_DISPLAY				EQU 	FFF5h
TEMP_COUNT 				EQU 	FFF6h
TEMP_CTRL 				EQU 	FFF7h
LEDS 					EQU 	FFF8h
IO_CURSOR				EQU		FFFCh
IO_WRITE				EQU 	FFFEh

;----------------------------------------------------
;				VARIAVEIS
;----------------------------------------------------

						ORIG 	8000h

ni_random				WORD 	0
jogada 					WORD  	0						;guarda jogada
codigo 					WORD 	0						;guarda codigo
resultado 				WORD 	0						;guarda comparacao
n_jogadas				WORD	0						;pontuacao atual
highscore				WORD	15						;highscore
limite 					WORD 	12						;limite jogadas num jogo
cursor 					WORD 	0000h					;posicao cursor
ganhou 					WORD 	0 						;flag

;________________strings_____________________________

menu_inicial 			STR 	'Carregue no botao IA para iniciar', STR_END
highscore_string		STR 	'HIGHSCORE:', STR_END
menu_final				STR 	'Fim do Jogo   Carregue em IA para recomecar ou em IB para sair', STR_END
creditos 				STR 	'Creditos:', STR_END
Andre 					STR 	'Andre Afonso Nunes Silva - 89408', STR_END
Rafael 					STR 	'Rafael Nunes Henriques   - 89530', STR_END
perdeu_str 				STR 	'PERDEU', STR_END
ganhou_str 				STR 	'GANHOU', STR_END

;________________end_interrupcoes____________________

						ORIG 	FE01h

INT1					WORD	INT1F
INT2					WORD	INT2F
INT3					WORD	INT3F
INT4					WORD	INT4F
INT5					WORD	INT5F
INT6					WORD	INT6F

						ORIG 	FE0Ah

INT10 					WORD 	INT10F
INT11 					WORD 	INT11F

						ORIG 	FE0Fh

INT15 					WORD 	INT15F


;----------------------------------------------------
;				CODIGO
;----------------------------------------------------

						ORIG 	0000h

						MOV 	R7, SP_INICIAL
						MOV 	SP, R7					;inicializa SP
						MOV 	R7, FFFFh
						MOV 	M[IO_CURSOR], R7 		;inicializa cursor IO

						JMP 	INICIO_JOGO

;----------------------------------------------------
;				INTERRUPCOES
;----------------------------------------------------

;_____________int_jogada____________________________
;
;Colocam o digito correspondente a interrupcao no registos
; onde esta a ser guardada a jogada

INT1F: 					SHL 	R1, 3
						ADD 	R1, 1
						RTI

INT2F: 					SHL 	R1, 3
						ADD 	R1, 2
						RTI

INT3F: 					SHL 	R1, 3
						ADD 	R1, 3
						RTI

INT4F: 					SHL 	R1, 3
						ADD 	R1, 4
						RTI

INT5F: 					SHL 	R1, 3
						ADD 	R1, 5
						RTI

INT6F: 					SHL 	R1, 3
						ADD 	R1, 6
						RTI



INT10F: 				INC 	R6						;flag
						RTI

INT11F: 				DEC 	R6						;flag
						RTI

INT15F: 				INC 	R3						;flag
						CMP 	R6, R0					;se ja tiver chegado ao fim nao precisa de contar mais tempo
						BR.Z	salta
						MOV 	R7, 5
						MOV		M[TEMP_COUNT], R7
						MOV 	R7, 1
						MOV 	M[TEMP_CTRL], R7		;reinicializa contador para proximos 500ms
salta:					RTI

;----------------------------------------------------
;				FUNCOES
;----------------------------------------------------

;_______________clrscr_______________________________
;
;Funcao que limpa ecra, imprimindo o caracter ' ' para todas
;	as posicoes
;
;Input: - Output: -

clrscr: 				PUSH 	R1						;cursor
						PUSH 	R2						;caracter ' '
						PUSH 	R3						;contador
						PUSH 	R4						;auxiliar
						MOV 	R1, 0000h
						MOV 	R2, clrscreen
						MOV 	R3, 1896

ciclo_clrscr: 			CMP 	R3, R0
						BR.Z 	fim_clrscr				;imprime em todas as posicoes
						DEC 	R3
						MOV 	M[IO_CURSOR], R1
						MOV 	M[IO_WRITE], R2
						INC 	R1
						MOV 	R4, R1
						AND 	R4, 00FFh
						CMP 	R4, 78					;chegou ao fim da linha?
						BR.Z 	next_line_clrscr
						BR		ciclo_clrscr

next_line_clrscr: 		MOV 	R4, R1					;mete colunas a 0 e incrementa linha
						AND 	R4, FF00h
						ADD 	R4, 0100h
						MOV 	R1, R4
						BR 		ciclo_clrscr

fim_clrscr: 			POP 	R4
						POP 	R3
						POP 	R2
						POP 	R1
						RET

;_______________print_str_______________________________
;
;Funcao que escreve uma string no ecra, comecando na
;	posicao que recebe como argumento
;
;Input: string, posicao cursor Output: -

print_str: 				PUSH 	R1						;caracter a imprimir
						PUSH 	R2 						;cursor string
						PUSH 	R3						;cursor IO
						PUSH 	R4						;auxiliar

						MOV 	R2, M[SP+7]
						MOV 	R3, M[SP+6]

ciclo_prtstr: 			MOV 	R1, M[R2]
						CMP 	R1, STR_END				;ve se chegou ao fim da str
						BR.Z 	fim_prtstr
						MOV		M[IO_CURSOR], R3
						MOV 	M[IO_WRITE], R1
						INC 	R2
						INC 	R3
						MOV 	R4, R3
						AND 	R4, 00FFh
						CMP 	R4, 78					;chegou ao fim da linha?
						BR.Z 	next_line_prtscr
						BR 		ciclo_prtstr

next_line_prtscr: 		MOV 	R4, R3					;coluna a zero, linhas + 1
						AND 	R4, FF00h
						ADD 	R4, 0100h
						MOV 	R3, R4
						BR 		ciclo_prtstr

fim_prtstr: 			POP 	R4
						POP 	R3
						POP 	R2
						POP 	R1
						RETN 	2

;_______________print_texto_LCD___________________
;
;Funcao que inicializa o LCD e imprime "Highscore:  --"
;
;Input: - Output: -

print_texto_LCD: 		PUSH 	R1						;cursor LCD
						PUSH 	R2						;cursor da string
						PUSH 	R3						;auxiliar string
						MOV 	R1, 8000h
						MOV 	R2, highscore_string

ciclo_texto_LCD:		MOV 	R3, M[R2]
						CMP 	R3, STR_END
						BR.Z	fim_texto_LCD
						MOV 	M[LCD_CTRL], R1
						MOV 	M[LCD_DISPLAY], R3
						INC 	R2
						INC 	R1
						BR 		ciclo_texto_LCD

fim_texto_LCD: 			MOV 	R1, 800Ch				;posicao dos algarismos
						MOV 	R3, '-'
						MOV 	M[LCD_CTRL], R1
						MOV 	M[LCD_DISPLAY], R3
						INC 	R1
						MOV 	M[LCD_CTRL], R1
						MOV 	M[LCD_DISPLAY], R3

						POP 	R3
						POP 	R2
						POP 	R1

						RET

;_______________atualiza_LCD___________________
;
;Funcao que atualiza o valor impresso no LCD
;
;Input: inteiro Output: -

atualiza_LCD:			PUSH	R1 						;valor a imprimir
						PUSH 	R2						;dezenas
						PUSH 	R3						;auxiliar
						MOV     R1, M[SP+5]
						MOV 	R3, R0

						CMP 	R1, 10
						BR.N  	print_n_LCD				;se for menor que 10 imprime
						INC		R2						;dezenas
						SUB 	R1,10

print_n_LCD:			ADD 	R2, 48					;converte em ASCII
						ADD 	R1, 48

						MOV 	R3, 800Ch
						MOV 	M[LCD_CTRL], R3
						MOV 	M[LCD_DISPLAY],R2		;imprime dezenas
						INC 	R3
						MOV 	M[LCD_CTRL], R3
						MOV 	M[LCD_DISPLAY],R1		;impime unidades

						POP 	R3
						POP 	R2
						POP		R1

						RETN 	1

;_______________input_jogada___________________
;
;Funcao que espera pela jogada do jogador e
;	devolve o input (jogada)
;
;Input: - Output: jogada

input_jogada:			PUSH 	R1						;jogada
						PUSH 	R2						;auxiliar
						PUSH 	R3						;flag do temporizador
						PUSH 	R6						;estado dos leds
						PUSH 	R7						;auxiliar

						MOV 	R7, 807Eh
						MOV 	M[INT_MASK_ADDR], R7 	;mascara interrutores
						MOV 	R6, FFFFh
						MOV 	M[LEDS], R6				;inicializa leds
						MOV 	R1, R0					;inicializa jogada
						MOV 	R3, R0					;flag a 0
						ENI
						MOV 	R7, 5
						MOV		M[TEMP_COUNT], R7
						MOV 	R7, 1
						MOV 	M[TEMP_CTRL], R7		;inicializa contador

ciclo_input:			MOV 	R2, R1
						AND 	R2, 0000111000000000b
						CMP 	R2, R0					;verifica se ja foram
						JMP.NZ	controlo_input			;introduzidos 4 algarismos
						CMP 	R3, 1
						BR.N	ciclo_input				;ja passou 500ms?
						CMP 	R6, R0
						JMP.Z	controlo_input			;ja acabou o tempo?
						SHR 	R6, 1
						MOV 	M[LEDS], R6
						MOV 	R3, R0
						BR 		ciclo_input

controlo_input:			CMP 	R2, R0
						BR.NZ 	fim_input				;foram introduzidos 4 algarismos?
						MOV 	R1, R0					;se nao mete a 0

fim_input:				DSI
						MOV 	M[SP+7], R1				;guarda jogada introduzida
						MOV 	M[LEDS], R0				;apaga os LEDS

						POP 	R7
						POP		R6
						POP 	R3
						POP 	R2
						POP 	R1

						RET

;_______________random_______________________________
;
;Funcao que gera um codigo pseudo-aleatorio
;
;Input: ni_inicial Output: ni_final , codigo

random:					PUSH 	R4 						;guarda os registos utilizados
						PUSH 	R7
						PUSH 	R6

						MOV 	R6, 4					;contador

random_loop: 			MOV 	R4, M[SP+5]				;ni inicial

						AND 	R4, 0001h				;if(n0 == 0)
						CMP 	R4, R0
						JMP.Z 	random_zero
						JMP 	random_else

random_zero: 			ROR 	M[SP+5], 1				;rotate_right(ni)
						JMP 	random_divisao

random_else: 			MOV 	R4, M[SP+5]				;rotate_right(xor(ni,mascara))
						XOR 	R4, MASCARA_RANDOM
						ROR 	R4, 1
						MOV 	M[SP+5], R4
						JMP 	random_divisao

random_divisao: 		MOV 	R4, M[SP+5]				;divisao de ni por M(6) para retirar o resto(algarismo)
						MOV 	R7, 6
						DIV 	R4, R7
						INC 	R7						;resto da divisao por M(6) da um valor entre 0 e 5, logo e preciso incrementar por 1
						ADD 	M[SP+6], R7

						DEC 	R6						;ja fez os 4?
						CMP 	R6, R0
						JMP.Z  	random_fim

						SHL		M[SP+6], 3				;continua para o proximo
						JMP 	random_loop

random_fim: 			POP 	R6						;reposicao dos registos utlizados
						POP 	R7
						POP 	R4

						RET

;_______________sete_seg_______________________________
;
;Funcao que atualiza o display de 7 segmentos (n jogadas atual)
;
;Input: inteiro Output: -

sete_seg:				PUSH 	R1
						PUSH 	R2

						MOV 	R1, M[SP+4]				;valor a imprimir
						MOV 	R2, R0 					;dezenas

ciclo_sete_seg:			CMP 	R1, 10 					;separa numero para imprimir
						JMP.NN 	maior_10				; em decimal
						JMP 	print_sete_seg

maior_10: 				INC 	R2
						SUB 	R1, 10
						JMP 	ciclo_sete_seg

print_sete_seg:			MOV 	M[IO_DISPLAY_4], R0		;limpa
						MOV 	M[IO_DISPLAY_3], R0		;limpa
						MOV 	M[IO_DISPLAY_2], R2		;print dezenas
						MOV 	M[IO_DISPLAY_1], R1		;print unidades

						POP 	R2
						POP 	R1

						RETN 	1

;_______________verifica_______________________________
;
;Funcao que compara a jogada com o codigo
; 	Tem duas funcoes auxiliares para verificar os certos e os existentes
;
;Input: jogada, codigo Output: resultado

verifica: 				PUSH 	R1						;codigo
						PUSH 	R2						;jogada
						PUSH 	R3						;guarda resultado

						MOV 	R1, M[SP+6]
						MOV 	R2, M[SP+5]
						MOV 	R3, R0

						PUSH 	R0
						PUSH 	R0
						PUSH 	R0
						PUSH 	R1
						PUSH 	R2
						PUSH 	R3
						CALL 	ver_x
						POP 	R3						;resultado
						POP 	R2						;jogada sem os certos
						POP 	R1						;codigo sem os certos

						PUSH 	R0
						PUSH 	R1
						PUSH 	R2
						PUSH 	R3
						CALL 	ver_o
						POP 	R3
						MOV 	M[SP+7], R3

						POP 	R3
						POP 	R2
						POP 	R1

						RETN 	2

ver_x:		 			PUSH 	R1						;codigo
						PUSH 	R2						;jogada
						PUSH 	R3						;guarda resultado
						PUSH 	R4 						;digito 1
						PUSH 	R5						;digito 2
						PUSH 	R6						;contador

						MOV 	R1, M[SP+10]
						MOV 	R2, M[SP+9]
						MOV 	R3, M[SP+8]
						MOV 	R6, R0

loop_ver_x: 			CMP 	R6, 4
						BR.Z 	fim_ver_x				;roda 4 vezes

						MOV 	R4, R1
						AND 	R4, 0007h				;isola algarismos direita
						MOV 	R5, R2
						AND 	R5, 0007h

						CMP 	R5, R4
						BR.NZ 	continua_x
						ROL 	R3, 2
						INC 	R3
						AND 	R1, FFF8h				;apaga alagarimos
						AND 	R2, FFF8h				;apaga algarismos

continua_x: 			ROR 	R1, 3					;proximos 2 digitos
						ROR 	R2, 3
						INC 	R6
						BR 		loop_ver_x

fim_ver_x: 				ROR 	R1, 4					;coloca codigo e jogada na posicao certa
						ROR 	R2, 4

						MOV 	M[SP+11], R3
						MOV 	M[SP+12], R2
						MOV 	M[SP+13], R1 			;mete resultados na stack

						POP 	R6
						POP 	R5
						POP 	R4
						POP 	R3
						POP 	R2
						POP 	R1

						RETN 	3

ver_o: 					PUSH 	R1						;codigo
						PUSH 	R2						;jogada
						PUSH 	R3						;resultado
						PUSH 	R4 						;digito 1
						PUSH 	R5						;digito 2
						PUSH 	R6						;contador

						MOV 	R1, M[SP+10]
						MOV 	R2, M[SP+9]
						MOV 	R3, M[SP+8]

ciclo_codigo: 			CMP 	R1, R0
						BR.Z 	fim_ver_o				;salta se ja encontrou 4 correspondencias

						MOV 	R4, R1
						AND 	R4, 0007h				;isola o algarismo da direita
						CMP 	R4, R0					;se estiver a 0 poupa verificacao
						BR.Z 	fim_ciclo_cod

						MOV 	R6, R0 					;contador

ciclo_jogada: 			CMP 	R6, 4					;ja viu os 4 algarismos
						BR.Z 	fim_ciclo_jog

						MOV 	R5, R2
						AND 	R5, 0007h
						CMP 	R5, R0					;se estiver a 0 passa para proximo
						BR.Z 	proximo_digito_jog

						CMP 	R5, R4
						BR.NZ 	proximo_digito_jog
						ROL 	R3, 2
						ADD 	R3, 2					;encontrou correspondencia
						AND 	R1, FFF8h				;apaga digitos direita
						AND 	R2, FFF8h

proximo_digito_jog:		ROR 	R2, 3					;proximo digito jogada
						INC 	R6						;contador aumenta
						BR 		ciclo_jogada

fim_ciclo_jog:	 		ROR 	R2, 4					;reposiciona os bits da jogada

fim_ciclo_cod: 			SHR 	R1, 3					;proximo digito codigo
						BR 		ciclo_codigo

fim_ver_o: 				MOV 	M[SP+11], R3 			;guarda resultado

						POP 	R6
						POP 	R5
						POP 	R4
						POP 	R3
						POP 	R2
						POP 	R1

						RETN 	3

;_______________print_resultado_______________________________
;
;Funcao que imprime jogada e o resultado da comparacao
;
;Input: cursor, resultado, jogada Output: cursor

print_resultado: 		PUSH	R1						;cursor
						PUSH	R2						;resultado
						PUSH 	R3						;jogada
						PUSH 	R4						;contador
						PUSH 	R5						;auxiliar

						MOV 	R1, M[SP+9]
						ADD 	R1, 0100h 				;imprime na linha seguinte
						AND 	R1, FF00h 				; na primeira coluna
						MOV 	R2, M[SP+8]
						MOV 	R3, M[SP+7]
						MOV 	R4, R0

						ROL 	R3, 4					;coloca jogada em posicao para imprimir

ciclo_prt_jog: 			CMP 	R4, 4					;ja imprimiu os 4?
						BR.Z 	prt_xo

						ROL 	R3, 3					;proximo digito
						MOV 	R5, R3
						AND 	R5, 0007h
						ADD 	R5, 48
						MOV 	M[IO_CURSOR], R1		;imprime alagrismo
						MOV 	M[IO_WRITE], R5
						INC 	R1						;proxima coluna
						INC 	R4						;aumenta contador

						BR 		ciclo_prt_jog

prt_xo: 				MOV 	R5, clrscreen
						MOV 	M[IO_CURSOR], R1
						MOV 	M[IO_WRITE], R5
						INC 	R1
						MOV 	M[IO_CURSOR], R1
						MOV 	M[IO_WRITE], R5			;dois espacos em branco

						MOV 	R4, R0
						ROL 	R2, 8					;prepara resultado para imprimir
ciclo_posiciona: 		CMP 	R4, 4					;se rodou 4 vezes ja esta posicionado
						BR.Z 	salta_posiciona
						MOV 	R5, R2
						AND 	R5, C000h				;isola os dois ultimos bits
						CMP 	R5, R0
						BR.NZ 	salta_posiciona			;se n e zero entao ja esta posicionado
						ROL 	R2, 2
						INC 	R4
						BR 		ciclo_posiciona

salta_posiciona:		MOV 	R4, R0

ciclo_prt_xo:	 		CMP 	R4, 4 					;ja imprimiu os 4?
						JMP.Z 	fim_prt_res

						ROL		R2, 2					;proximos 2 bits
						MOV 	R5, R2
						AND 	R5, 0003h 				;isola os dois bits
						CMP 	R5, 0001h
						BR.Z 	print_x					;se for zero, algarismo e 1, imprime 'x'
						BR.P 	print_o 				;se for positivo, algarismo e 2, imprime 'o'
						BR 		print_traco 			;se for negativo, algarismo e 0, imprime '-'

print_x: 				MOV 	R5, 'x'					;imprime 'x'
						MOV 	M[IO_CURSOR], R1
						MOV 	M[IO_WRITE], R5
						INC 	R1
						INC 	R4
						BR 		ciclo_prt_xo

print_o: 				MOV 	R5, 'o'					;imrprime 'o'
						MOV 	M[IO_CURSOR], R1
						MOV 	M[IO_WRITE], R5
						INC 	R1
						INC 	R4
						BR 		ciclo_prt_xo

print_traco: 			MOV 	R5, '-'					;imprime '-'
						MOV 	M[IO_CURSOR], R1
						MOV 	M[IO_WRITE], R5
						INC 	R1
						INC 	R4
						JMP		ciclo_prt_xo

fim_prt_res: 			MOV 	M[SP+10], R1 			;guarda cursor

						POP 	R5
						POP 	R4
						POP 	R3
						POP 	R2
						POP 	R1

						RETN 	3


;----------------------------------------------------
;				CORPO PRINCIPAL
;----------------------------------------------------

INICIO_JOGO:			CALL 	clrscr					;limpa ecra

						CALL 	print_texto_LCD			;inicializa LCD

						PUSH 	menu_inicial
						PUSH 	0000h 					;posicao inicial cursos
						CALL 	print_str


						MOV 	R7, 0400h
						MOV 	M[INT_MASK_ADDR], R7
						ENI
						MOV 	R6, R0
ciclo_IA:				INC 	M[ni_random]			;incrementa o ni enquanto espera pela interrupcao
						CMP 	R6, 1
						BR.NZ 	ciclo_IA
						DSI

JOGO_NOVO:				PUSH 	R0
						PUSH 	M[ni_random]
						CALL 	random 					;gera codigo
						POP 	M[ni_random]
						POP 	M[codigo]

						CALL 	clrscr

						MOV 	M[n_jogadas], R0
						PUSH 	M[n_jogadas]
						CALL 	sete_seg				;inicializa display 7 segmentos

INICIO_JOGADA: 			PUSH	R0
						CALL 	input_jogada
						POP 	M[jogada] 				;recebe jogada
						CMP 	M[jogada], R0			;verifica se foi introduzida
						JMP.Z 	novo_jogo				;se for 0 o tempo acabou acaba o jogo

						PUSH 	R0
						PUSH	M[codigo]
						PUSH 	M[jogada]
						CALL 	verifica				;compara jogada e codigo
						POP 	M[resultado]			;recebe resultado

print_jogada:			PUSH 	R0						;guarda espaco para cursor final
						PUSH 	M[cursor]
						PUSH 	M[resultado]
						PUSH 	M[jogada]				;codificado em 2 bits cada
						CALL 	print_resultado			;print jogada e do resultado
						POP 	M[cursor]

						INC 	M[n_jogadas]
						MOV 	R7, 0055h
						CMP 	M[resultado], R7 		;verifica se ganhou
						BR.Z	atualiza_highscore
						PUSH 	M[n_jogadas]
						CALL 	sete_seg 				;atualiza display
						MOV 	R7, M[n_jogadas]
						CMP 	R7, 12
						BR.Z	novo_jogo				;se ja fez 12 jogadas

						MOV 	M[jogada], R0			;reinicializa jogada
						MOV 	M[resultado], R0		;reinicializa resultado

						JMP		INICIO_JOGADA

atualiza_highscore: 	INC 	M[ganhou]
						MOV 	R7, M[n_jogadas]
						CMP 	R7, M[highscore]
						BR.NN 	novo_jogo				;se tiver feito highscore
						MOV 	M[highscore], R7		;atualiza highscore
						PUSH 	R7
						CALL 	atualiza_LCD			;atualiza LCD

novo_jogo: 				CALL 	clrscr 					;continua para menu final

						CMP 	M[ganhou], R0 			;ganhou ou perdeu?
						BR.Z 	perdeu
						MOV 	R7, ganhou_str
						MOV 	M[ganhou], R0
						BR 		salta_ganhou
perdeu: 				MOV 	R7, perdeu_str
salta_ganhou: 			PUSH 	R7
						PUSH 	R0
						CALL 	print_str				;imprime GANHOU ou PERDEU no IO

						PUSH 	menu_final
						PUSH 	0100h
						CALL 	print_str   			;print menu final

						MOV 	R7, 0C00h
						MOV 	M[INT_MASK_ADDR], R7
						ENI
						MOV 	R6, R0
ciclo_IA_IB: 			CMP 	R6, FFFFh 				;escolheu IA ou IB?
						BR.Z 	fim_creditos
						CMP 	R6, 0001h
						BR.NZ 	ciclo_IA_IB
						DSI

						MOV 	M[jogada], R0 			;repoe valores para novo jogo
						MOV 	M[codigo], R0
						MOV 	M[resultado], R0
						MOV 	M[n_jogadas], R0
						MOV 	M[cursor], R0

						JMP 	JOGO_NOVO

fim_creditos: 			DSI
						CALL 	clrscr

						PUSH 	creditos
						PUSH 	R0
						CALL 	print_str 				;imprime creditos e acabou

						PUSH 	Andre
						PUSH 	0100h
						CALL 	print_str

						PUSH 	Rafael
						PUSH 	0200h
						CALL 	print_str

Fim: 					BR 		Fim