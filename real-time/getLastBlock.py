# coding=utf-8
import json
import requests
import datetime
from time import sleep 
from operator import mod
import py2neo
from py2neo import Graph, Node, Relationship


blockchain_db = Graph("http://localhost:7474/db/data/", user="neo4j", password="123456")

urlLastBlock = "https://blockchain.info/es/latestblock"
urlBlock = "https://blockchain.info/es/rawblock/"
urlTransaction = "https://blockchain.info/es/rawtx/"

lastIndexSaved = 0

previousBlockHash = ''



class Block(object):
	def __init__(self,magicID,blockSize,blockHeader,transactionsCount,hashHeader,hashHeaderReduced,timeStamp,version):
		self.magicID = magicID
		self.blockSize = blockSize
		self.blockHeader = blockHeader
		self.transactionsCount = transactionsCount
		self.hashHeader = hashHeader
		self.hashHeaderReduced = hashHeaderReduced
		self.timeStamp = timeStamp
		self.version = version

class Transaction(object):
	def __init__(self,transactionVersion,inputCount,outputCount,lockTime,hashTransaction,hashTransactionReduced):
		self.transactionVersion = transactionVersion
		self.inputCount = inputCount
		self.outputCount = outputCount
		self.lockTime = lockTime
		self.hashTransaction = hashTransaction
		self.hashTransactionReduced = hashTransactionReduced

class Input(object):
	def __init__(self,indexPreviousTxout,scriptLength,script,sequenceNumber,hashPreviousTransactionReduced):
		self.indexPreviousTxout = indexPreviousTxout
		self.scriptLength = scriptLength
		self.script = script
		self.sequenceNumber = sequenceNumber
		self.hashPreviousTransactionReduced = hashPreviousTransactionReduced

class Output(object):
	def __init__(self,valueSatoshis,scriptLength,lockingScript,indexTxOut):
		self.valueSatoshis = valueSatoshis
		self.scriptLength = scriptLength
		self.lockingScript = lockingScript
		self.indexTxOut = indexTxOut


def getJSON(url):
	try:
		r = requests.get(url=url)
	except requests.exceptions.ConnectionError:
		print "connection error"
		sleep(1)
		return False
	return r.json()

def storeNodes(newBlockToSave, transactions, inputs, outputs):
	tx = blockchain_db.begin()
	newBlockNode = Node("Block", magicId=newBlockToSave.magicID, blockSize=newBlockToSave.blockSize, 
					blockHeader=newBlockToSave.blockHeader, transactionsCount=newBlockToSave.transactionsCount, 
					hashHeader=newBlockToSave.hashHeader, hashHeaderReduced=newBlockToSave.hashHeaderReduced,
					timeStamp=newBlockToSave.timeStamp, version=newBlockToSave.version)
	tx.create(newBlockNode)
	for transaction in transactions:
		inputsWithoutOrigin = []
		inputsNodesWithoutOrigin = []

		transactionNode = Node("Transaction", transactionVersion=transaction.transactionVersion, 
					inputCount=transaction.inputCount, outputCount=transaction.outputCount, 
					lockTime=transaction.lockTime, hashTransaction=transaction.hashTransaction,
					hashTransactionReduced=transaction.hashTransactionReduced)
		tx.create(transactionNode)

		for inputObj in inputs:
			inputNode = Node("Input", indexPreviousTxout=inputObj.indexPreviousTxout, scriptLength=inputObj.scriptLength,
					script=inputObj.script, sequenceNumber=inputObj.sequenceNumber,
					hashPreviousTransactionReduced=inputObj.hashPreviousTransactionReduced)
			tx.create(inputNode)
			tx.create(Relationship(inputNode, transactionNode))
			if(transaction.inputCount > 1):
				previousOutput = blockchain_db.data("OPTIONAL MATCH (t:Transaction {hashTransactionReduced: {hashPreviousTransactionReduced}})<-[:TO]-(out:Output) WHERE out.indexTxOut={indexPreviousTxout} RETURN out", 
							hashPreviousTransactionReduced=inputObj.hashPreviousTransactionReduced, 
							indexPreviousTxout=inputObj.indexPreviousTxout)
				previousOutputNode = previousOutput[0]['out']
				if (previousOutputNode != None):
					tx.create(Relationship(inputNode,'ORIGIN_OUTPUT',previousOutputNode))
				else:
					inputsWithoutOrigin.append(inputObj)
					inputsNodesWithoutOrigin.append(inputNode)

		for outputObj in outputs:
			outputNode = Node("Output", valueSatoshis=outputObj.valueSatoshis, scriptLength=outputObj.scriptLength,
					lockingScript=outputObj.lockingScript, indexTxOut=outputObj.indexTxOut)
			tx.create(Relationship(outputNode, transactionNode))

		tx.create(Relationship(transactionNode, newBlockNode))
	
	tx.commit()

	# Ahora se intentan relacionar los nodos input a los que no se ha encontrado origen porque esta en el mismo bloque
	for k in xrange(len(inputsWithoutOrigin)):
		previousOutput = blockchain_db.data("OPTIONAL MATCH (t:Transaction {hashTransactionReduced: {hashPreviousTransactionReduced}})<-[:TO]-(out:Output) WHERE out.indexTxOut={indexPreviousTxout} RETURN out", 
					hashPreviousTransactionReduced=inputsWithoutOrigin[k].hashPreviousTransactionReduced, 
					indexPreviousTxout=inputsWithoutOrigin[k].indexPreviousTxout)
		previousOutputNode = previousOutput[0]['out']
		if (previousOutputNode != None):
			originOutRelation = Relationship(inputsNodesWithoutOrigin[k],'ORIGIN_OUTPUT',previousOutputNode)
			blockchain_db.create(originOutRelation)
		else:
			print "Se sigue sin guardar"

	hashHeaderReduced = newBlockToSave.hashHeaderReduced
	previousChainBlock = blockchain_db.find_one('Block', property_key='hashHeaderReduced', property_value=hashHeaderReduced)
	if(previousChainBlock != None):
		prevBlockRelation = Relationship(newBlockNode,'PREVIOUS_BLOCK',previousChainBlock)
		blockchain_db.create(prevBlockRelation)

def getBlock(blockIndex):
	urlBlockToSave = ''.join([urlBlock,str(blockIndex)])
	print urlBlockToSave
	blockToSave = getJSON(urlBlockToSave)
	while(not blockToSave):
		blockToSave = getJSON(urlBlockToSave)
	transactions = []
	global previousBlockHash
	global lastIndexSaved

	# Comprobar con el bloque genesis que todos los datos están en orden
	# Extracción de los datos
	magicID = 'd9b4bef9'
	blockSize = str(blockToSave["size"])
	#blockHeader = blockToSave[""]
	transactionsCount = str(blockToSave["n_tx"])
	print "Numero de transacciones:",transactionsCount
	hashHeader = str(blockToSave["hash"])
	hashHeaderReduced = hashHeader[57::]
	timeStamp = str(blockToSave["time"])
	version = str(blockToSave["ver"])

	# Extracción del Header : version+previousBlockHash+merkleRoot+timeStamp+(difficultyTarget->No está en la API)nonce
	previousBlockHash = blockToSave["prev_block"]
	merkleRoot = blockToSave["mrkl_root"]
	nonce = str(blockToSave["nonce"])
	blockHeader = ''.join([version,previousBlockHash,merkleRoot,timeStamp,nonce])

	newBlockToSave = Block(magicID,blockSize,blockHeader,transactionsCount,hashHeader,hashHeaderReduced,timeStamp,version)

	counterTx = 0
	for transaction in blockToSave["tx"]:
		outputs = []
		inputs = []
		transactionVersion = str(transaction["ver"])
		inputCount = str(len(transaction["inputs"]))
		outputCount = str(len(transaction["out"]))
		lockTime = str(transaction["lock_time"])	
		hashTransaction = str(transaction["hash"])
		hashTransactionReduced = hashTransaction[57::]

		transactionObject = Transaction(transactionVersion,inputCount,outputCount,lockTime,hashTransaction,hashTransactionReduced) 
		transactions.append(transactionObject)

		for inputN in transaction["inputs"]:
			if(len(list(inputN.keys()))>2):
				indexPreviousTxout = str(hex(inputN["prev_out"]["n"])) # PENSAR SI ES MEJOR SIEMPRE EN INT(Habría que modificar guardado)
				script = str(inputN["script"])
				scriptLength = str(len(script)/2)  
				sequenceNumber = str(hex(inputN["sequence"]))
				urlPreviousTx = ''.join([urlTransaction,str(inputN["prev_out"]["tx_index"])])
				previousTx = getJSON(urlPreviousTx)
				while(not previousTx):
					previousTx = getJSON(urlPreviousTx)
				hashPreviousTransactionReduced = str(previousTx["hash"])[57::]
			else:
				indexPreviousTxout = ''.join(['f']*8)
				script = str(inputN["script"])
				scriptLength = str(len(script)/2)
				sequenceNumber = str(hex(inputN["sequence"]))
				hashPreviousTransactionReduced = '0'*7
			inputs.append(Input(indexPreviousTxout,scriptLength,script,sequenceNumber,hashPreviousTransactionReduced))

		for output in transaction["out"]:
			valueSatoshis = str(output["value"])
			lockingScript = str(output["script"])
			scriptLength = str(len(lockingScript)/2) 
			indexTxOut = str(hex(output["n"])) # PENSAR SI ES MEJOR SIEMPRE EN INT(Habría que modificar guardado)
			outputs.append(Output(valueSatoshis,scriptLength,lockingScript,indexTxOut))
		counterTx += 1
		if(mod(counterTx,100)==0):
			print counterTx

	lastIndexSaved += 1
	fileIndex = open('indexBlock.txt', 'w')
	fileIndex.write(str(lastIndexSaved))
	fileIndex.close()

	storeNodes(newBlockToSave,transactions,inputs,outputs)

	


def main():
	lastBlockMined = getJSON(urlLastBlock)
	while(not lastBlockMined):
		lastBlockMined = getJSON(urlLastBlock)
	indexLastBlockMined = lastBlockMined["block_index"]
	print indexLastBlockMined
	global lastIndexSaved
	fileIndex = open('indexBlock.txt', 'r')
	lastIndexSaved = int(fileIndex.readline().rstrip())
	fileIndex.close()
	while(lastIndexSaved<indexLastBlockMined):
		getBlock(lastIndexSaved+1)
	main()


timestart = datetime.datetime.now()
main()
timefinish = datetime.datetime.now()
print "Ha tardado:", (timefinish-timestart)
