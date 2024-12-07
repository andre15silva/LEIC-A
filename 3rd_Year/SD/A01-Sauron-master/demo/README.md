# Guião de Demonstração

## 1. Preparação do Sistema

Para testar a aplicação e todos os seus componentes, é necessário preparar um ambiente com dados para proceder à verificação dos testes.

### 1.1. Compilar o Projeto

Primeiramente, é necessário instalar as dependências necessárias para o *silo* e os clientes (*eye* e *spotter*) e compilar estes componentes.
Para isso, basta ir à diretoria *root* do projeto e correr o seguinte comando:

```
$ mvn clean install -DskipTests
```

Com este comando já é possível analisar se o projeto compila na íntegra.

### 1.2. *ZooKeeper*

Para podermos lançar as réplicas do servidor é necessário haver um servidor de nomes ZooKeeper a correr.
É por isso necessário lançar o servidor ZooKeeper.

Vamos assumir, de agora em diante, que o ZooKeeper foi lançado no endereço *localhost* e na porta *2181*.

### 1.2. *Silo*

Para proceder aos testes, é preciso que pelo menos uma réplica do servidor *silo* esteja a correr. 
Para isso basta ir à diretoria *silo-server* e executar:

```
$ mvn exec:java
```

Este comando vai lançar uma réplica com o nome */grpc/sauron/silo/1*, endereço *localhost* na porta *8081* e que vai contactar o servidor de nomes ZooKeeper no endereço e porta assumidos.

### 1.3. *Eye*

Vamos registar 3 câmeras e as respetivas observações. 
Cada câmera vai ter o seu ficheiro de entrada próprio com observações já definidas.
Para isso basta ir à diretoria *eye* e correr os seguintes comandos:

```
$ eye localhost 2181 Tagus 38.737613 -9.303164 < eye1.txt
$ eye localhost 2181 Alameda 30.303164 -10.737613 < eye2.txt
$ eye localhost 2181 Lisboa 32.737613 -15.303164 < eye3.txt
```
**Nota:** Para correr o script *eye* é necessário fazer `mvn install` e adicionar ao *PATH* ou utilizar diretamente os executáveis gerados na diretoria `target/appassembler/bin/`.

Depois de executar os comandos acima já temos o que é necessário para testar o sistema. 

## 2. Teste das Operações

Nesta secção vamos correr os comandos necessários para testar todas as operações.
Cada subsecção é respetiva a cada operação presente no *silo*.

### 2.1. *cam_join*

Esta operação já foi testada na preparação do ambiente, no entanto ainda é necessário testar algumas restrições.

2.1.1. Teste das câmeras com nome duplicado e coordenadas diferentes.  
O servidor deve rejeitar esta operação. 
Para isso basta executar um *eye* com o seguinte comando:

```
$ eye localhost 8080 Tagus 10.0 10.0
```

2.1.2. Teste do tamanho do nome.  
O servidor deve rejeitar esta operação. 
Para isso basta executar um *eye* com o seguinte comando:

```
$ eye localhost 8080 ab 10.0 10.0
$ eye localhost 8080 abcdefghijklmnop 10.0 10.0
```

### 2.2. *cam_info*

Esta operação não tem nenhum comando específico associado e para isso é necessário ver qual o nome do comando associado a esta operação. 
Para isso precisamos instanciar um cliente *spotter*, presente na diretoria com o mesmo nome:

```
$ mvn exec:java
```

Este cliente *spotter* vai contactar o servidor de nomes no enderenço e porta assumidos.

De seguida, corremos o comando *help* e, **assumindo** que o comando se chama *info* e recebe um nome, corremos os seguintes testes:

```
> help
```

2.2.1. Teste para uma câmera existente.  
O servidor deve responder com as coordenadas de localização da câmera *Tagus* (38.737613 -9.303164):

```
> info Tagus
```

2.2.2. Teste para câmera inexistente.  
O servidor deve rejeitar esta operação:

```
> info Inexistente
```

### 2.3. *report*

Esta operação já foi testada acima na preparação do ambiente.

No entanto falta testar o sucesso do comando *zzz*. 
Na preparação foi adicionada informação que permite testar este comando.
Para testar basta abrir um cliente *spotter* e correr o comando seguinte:

```
> trail car 00AA00
```

O resultado desta operação deve ser duas observações pela câmera *Tagus* com intervalo de mais ou menos 5 segundos.

### 2.4. *track*

Esta operação vai ser testada utilizando o comando *spot* com um identificador.

2.4.1. Teste com uma pessoa (deve devolver vazio):

```
> spot person 14388236
```

2.4.2. Teste com uma pessoa:

```
> spot person 123456789
person,123456789,<timestamp>,Alameda,30.303164,-10.737613
```

2.4.3. Teste com um carro:

```
> spot car 20SD21
car,20SD21,<timestamp>,Alameda,30.303164,-10.737613
```

### 2.5. *trackMatch*

Esta operação vai ser testada utilizando o comando *spot* com um fragmento de identificador.

2.5.1. Teste com uma pessoa (deve devolver vazio):

```
> spot person 143882*
```

2.5.2. Testes com uma pessoa:

```
> spot person 111*
person,111111000,<timestamp>,Tagus,38.737613,-9.303164

> spot person *000
person,111111000,<timestamp>,Tagus,38.737613,-9.303164

> spot person 111*000
person,111111000,<timestamp>,Tagus,38.737613,-9.303164
```

2.5.3. Testes com duas ou mais pessoas:

```
> spot person 123*
person,123111789,<timestamp>,Alameda,30.303164,-10.737613
person,123222789,<timestamp>,Alameda,30.303164,-10.737613
person,123456789,<timestamp>,Alameda,30.303164,-10.737613

> spot person *789
person,123111789,<timestamp>,Alameda,30.303164,-10.737613
person,123222789,<timestamp>,Alameda,30.303164,-10.737613
person,123456789,<timestamp>,Alameda,30.303164,-10.737613

> spot person 123*789
person,123111789,<timestamp>,Alameda,30.303164,-10.737613
person,123222789,<timestamp>,Alameda,30.303164,-10.737613
person,123456789,<timestamp>,Alameda,30.303164,-10.737613
```

2.5.4. Testes com um carro:

```
> spot car 00A*
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164

> spot car *A00
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164

> spot car 00*00
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164
```

2.5.5. Testes com dois ou mais carros:

```
> spot car 20SD*
car,20SD20,<timestamp>,Alameda,30.303164,-10.737613
car,20SD21,<timestamp>,Alameda,30.303164,-10.737613
car,20SD22,<timestamp>,Alameda,30.303164,-10.737613

> spot car *XY20
car,66XY20,<timestamp>,Lisboa,32.737613,-15.303164
car,67XY20,<timestamp>,Alameda,30.303164,-10.737613
car,68XY20,<timestamp>,Tagus,38.737613,-9.303164

> spot car 19SD*9
car,19SD19,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD29,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD39,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD49,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD59,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD69,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD79,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD89,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD99,<timestamp>,Lisboa,32.737613,-15.303164
```

### 2.6. *trace*

Esta operação vai ser testada utilizando o comando *trail* com um identificador.

2.6.1. Teste com uma pessoa (deve devolver vazio):

```
> trail person 14388236
```

2.6.2. Teste com uma pessoa:

```
> trail person 123456789
person,123456789,<timestamp>,Alameda,30.303164,-10.737613
person,123456789,<timestamp>,Alameda,30.303164,-10.737613
person,123456789,<timestamp>,Tagus,38.737613,-9.303164

```

2.6.3. Teste com um carro (deve devolver vazio):

```
> trail car 12XD34
```

2.6.4. Teste com um carro:

```
> trail car 00AA00
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164
```

## 3. Replicação e Tolerância a Faltas

### 3.1. *Lançar réplicas*

O procedimento para lançar várias réplicas é semelhante ao procedimento para lançar apenas uma réplica.
Para lançar 3 réplicas basta ir à diretoria *silo-server* e executar:

```
$ mvn exec:java
$ mvn exec:java -Dinstance=2
$ mvn exec:java -Dinstance=3
```

Estes comandos vão lançar três réplicas com os nomes */grpc/sauron/silo/${instance}*, endereço *localhost* na porta *808${instance}* e que vão contactar o servidor de nomes ZooKeeper no endereço e porta assumidos.
A periodicidade do *gossip* é o valor por omissão, 30 segundos, para todas estas réplicas.

### 3.2. *Fornecer dados*

Para fornecer dados às réplicas o procedimento é semelhante ao procedimento para submeter dados a apenas uma réplica.

Vamos registar 3 câmeras e as respetivas observações, uma em cada réplica. 
Para isso basta ir à diretoria *eye* e correr os seguintes comandos:

```
$ eye localhost 2181 Tagus 38.737613 -9.303164 1 < eye1.txt
$ eye localhost 2181 Alameda 30.303164 -10.737613 2 < eye2.txt
$ eye localhost 2181 Lisboa 32.737613 -15.303164 3 < eye3.txt
```
**Nota:** Para correr o script *eye* é necessário fazer `mvn install` e adicionar ao *PATH* ou utilizar diretamente os executáveis gerados na diretoria `target/appassembler/bin/`.

### 3.3. *Fazer interrogações*

Para fazer interrogações às réplicas o procedimento é semelhante ao procedimento para fazer interrogações a apenas uma réplica.

Executemos 3 spotters diferentes 

```
$ spotter localhost 2181 512 1
$ spotter localhost 2181 512 2
$ spotter localhost 2181 512 3
```
**Nota:** Para correr o script *spotter* é necessário fazer `mvn install` e adicionar ao *PATH* ou utilizar diretamente os executáveis gerados na diretoria `target/appassembler/bin/`.

Em seguida, pode-se executar qualquer operação de *2.2* a *2.6* em qualquer um dos spotters que consequentemente vai interagir com a sua respetiva réplica.

### 3.4. *Testar tolerância a faltas*

#### Situação 1
Reiniciar as 3 réplicas iniciadas em *3.1*.

Executar um eye:
```
$ eye localhost 2181 Tagus 38.737613 -9.303164 1
```

Desligar a réplica 1 com Ctrl+C antes desta propagar os seus updates.

Executar no eye:
```
person,1111111

```
O eye vai ligar-se a outra réplica enviando um novo *camJoin* e registando a nova observação.

#### Situação 2
Reiniciar as 3 réplicas iniciadas em *3.1*.

Executar um eye:
```
$ eye localhost 2181 Tagus 38.737613 -9.303164 1 < eye1.txt
```

Desligar a réplica 1 com Ctrl+Z antes desta propagar os seus updates.

Ligar um spotter à réplica 2:
```
$ mvn exec:java -Dinstance=2
```

Executar no spotter:
```
> info Tagus
No camera with name Tagus was found.
```
Executar ```fg``` para retomar a réplica 1.
Esperar que a réplica 1 propague a sua informação.

Executar no spotter:
```
> info Tagus
Camera: Tagus, Location: Lat(38.737613)  Long(-9.303164)
```

### 3.5. *Testar coerência de leituras*

Desligar as réplicas previamente ligadas e lançar 3 novas réplicas no servidor com intervalo de 10 minutos para executar os testes de forma mais fácil (evitar propagação)

```
$ mvn exec:java -Dgossip.interval=600
$ mvn exec:java -Dinstance=2 -Dgossip.interval=600
$ mvn exec:java -Dinstance=3 -Dgossip.interval=600
```

Em seguida ligar um eye à instância 1, utilizando:
```
$ eye localhost 2181 Tagus 38.737613 -9.303164 1 < eye1.txt
```

Executemos agora um spotter com o comando:
```
$ spotter localhost 2181 512 1
```

No spotter:
```
> spot person 123456789
person,123456789,<timestamp>,Alameda,30.303164,-10.737613

> info Tagus
Camera: Tagus, Location: Lat(38.737613)  Long(-9.303164)
```

Desligar a réplica 1 com Ctrl+C. O spotter vai, no próximo pedido, conectar-se a uma nova réplica aleatoriamente, que não terá ainda o estado atualizado.

No spotter:
```
> spot person 123456789
person,123456789,<timestamp>,Alameda,30.303164,-10.737613

> info Tagus
Camera: Tagus, Location: Lat(38.737613)  Long(-9.303164)
```

O cliente irá receber uma resposta desatualizada do servidor em ambos os casos, sendo os pedidos respondidos pela cache.

### 3.6. *Testar situações não toleradas*

#### Falhas definitivas das réplicas
Reiniciar as 3 réplicas iniciadas em *3.1*.

Em seguida ligar um eye à instância 1, utilizando:
```
$ eye localhost 2181 Tagus 38.737613 -9.303164 1 < eye1.txt
```

Desligar a réplica 1 antes de propagar com Ctrl+C, todo o estado é perdido.
O conteúdo registado pelo eye foi perdido e já não poderá ser lido pelo spotter.

#### Camêras com nome igual e coordenadas diferentes
Reiniciar as 3 réplicas iniciadas em *3.1*.

Em seguida ligar um eye à instância 1, utilizando:
```
$ eye localhost 2181 Tagus 38.737613 -9.303164 1
```

Antes da instância 1 propagar os seus updates, ligar um eye à instância 2, utilizando:
```
$ eye localhost 2181 Tagus 40 50 2
```

Os updates correspondentes a estas duas câmeras nunca vão ser aplicados nas réplicas opostas, uma vez que já existe uma câmera com aquele nome registada nelas.
Como estes updates não são aplicados, a réplica 1 vai deixar de atualizar o seu estado com as informações que a réplica 2 receber e vice-versa.
