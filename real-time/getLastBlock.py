# coding=utf-8
import json
import requests
import datetime
import py2neo
from py2neo import Graph, Node, Relationship


blockchain_db = Graph("http://localhost:7474/db/data/", user="neo4j", password="123456")

urlLastBlock = "https://blockchain.info/es/latestblock"
urlBlock = "https://blockchain.info/es/rawblock/"
urlTransaction = "https://blockchain.info/es/rawtx/"

lastIndexSaved = 0

previousBlockHash = ''


class Block(object):
	def __init__(self,magicID,blockSize,blockHeader,transactionsCount,hashHeader,timeStamp,version):
		self.magicID = magicID
		self.blockSize = blockSize
		self.blockHeader = blockHeader
		self.transactionsCount = transactionsCount
		self.hashHeader = hashHeader
		self.timeStamp = timeStamp
		self.version = version

class Transaction(object):
	def __init__(self,transactionVersion,inputCount,outputCount,lockTime,hashTransaction):
		self.transactionVersion = transactionVersion
		self.inputCount = inputCount
		self.outputCount = outputCount
		self.lockTime = lockTime
		self.hashTransaction = hashTransaction

class Input(object):
	def __init__(self,indexPreviousTxout,scriptLength,script,sequenceNumber,hashPreviousTransaction):
		self.indexPreviousTxout = indexPreviousTxout
		self.scriptLength = scriptLength
		self.script = script
		self.sequenceNumber = sequenceNumber
		self.hashPreviousTransaction = hashPreviousTransaction

class Output(object):
	def __init__(self,valueSatoshis,scriptLength,lockingScript,indexTxOut):
		self.valueSatoshis = valueSatoshis
		self.scriptLength = scriptLength
		self.lockingScript = lockingScript
		self.indexTxOut = indexTxOut


def getJSON(url):
	r = requests.get(url=url)
	return r.json()

def storeNodes(newBlockToSave, transactions, inputs, outputs):
	transactions = []

	tx = blockchain_db.begin()
	newBlockNode = Node("Block", magicId=newBlockToSave.magicID, blockSize=newBlockToSave.blockSize, 
					blockHeader=newBlockToSave.blockHeader, transactionsCount=newBlockToSave.transactionsCount, 
					hashHeader=newBlockToSave.hashHeader, timeStamp=newBlockToSave.timeStamp, version=newBlockToSave.version)
	tx.create(newBlockNode)
	for transaction in transactions:
		inputsWithoutOrigin = []
		inputsNodesWithoutOrigin = []
		outputs = []
		inputs = []

		transactionNode = Node("Transaction", transactionVersion=transactions.transactionVersion, 
					inputCount=transactions.inputCount, outputCount=transactions.outputCount, 
					lockTime=transactions.lockTime,
					hashTransaction=transactions.hashTransaction)
		tx.create(transactionNode)

		for inputObj in inputs:
			inputNode = Node("Input", indexPreviousTxout=inputObj.indexPreviousTxout, scriptLength=inputObj.scriptLength,
					script=inputObj.script, sequenceNumber=inputObj.sequenceNumber,
					hashPreviousTransaction=inputObj.hashPreviousTransaction)
			tx.create(inputNode)
			tx.create(Relationship(inputNode, transactionNode))
			if(transaction.inputCount > 1):
				previousOutput = blockchain_db.data("OPTIONAL MATCH (t:Transaction {hashTransaction: {hashPreviousTransaction}})<-[:TO]-(out:Output) WHERE out.indexTxOut={indexPreviousTxout} RETURN out", 
							hashPreviousTransaction=inputObj.hashPreviousTransaction, 
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
		previousOutput = blockchain_db.data("OPTIONAL MATCH (t:Transaction {hashTransaction: {hashPreviousTransaction}})<-[:TO]-(out:Output) WHERE out.indexTxOut={indexPreviousTxout} RETURN out", 
					hashPreviousTransaction=inputsWithoutOrigin[k].hashPreviousTransaction, 
					indexPreviousTxout=inputsWithoutOrigin[k].indexPreviousTxout)
		previousOutputNode = previousOutput[0]['out']
		if (previousOutputNode != None):
			originOutRelation = Relationship(inputsNodesWithoutOrigin[k],'ORIGIN_OUTPUT',previousOutputNode)
			blockchain_db.create(originOutRelation)
		else:
			print "Se sigue sin guardar"


	previousChainBlock = blockchain_db.find_one('Block', property_key='hashHeader', property_value=previousBlockHash)
	if(previousChainBlock != None):
		prevBlockRelation = Relationship(newBlockNode,'PREVIOUS_BLOCK',previousChainBlock)
		blockchain_db.create(prevBlockRelation)

def getBlock(blockIndex):
	urlBlockToSave = ''.join([urlBlock,str(blockIndex)])
	print urlBlockToSave
	blockToSave = getJSON(urlBlockToSave)

	global previousBlockHash

	# Comprobar con el bloque genesis que todos los datos están en orden
	# Extracción de los datos
	magicID = 'd9b4bef9'
	blockSize = str(blockToSave["size"])
	#blockHeader = blockToSave[""]
	transactionsCount = str(blockToSave["n_tx"])
	print "Numero de transacciones:",transactionsCount
	hashHeader = str(blockToSave["hash"])
	timeStamp = str(blockToSave["time"])
	version = str(blockToSave["ver"])

	# Extracción del Header : version+previousBlockHash+merkleRoot+timeStamp+(difficultyTarget->No está en la API)nonce
	previousBlockHash = blockToSave["prev_block"]
	merkleRoot = blockToSave["mrkl_root"]
	nonce = str(blockToSave["nonce"])
	blockHeader = ''.join([version,previousBlockHash,merkleRoot,timeStamp,nonce])

	newBlockToSave = Block(magicID,blockSize,blockHeader,transactionsCount,hashHeader,timeStamp,version)

	for transaction in blockToSave["tx"]:
		transactionVersion = str(transaction["ver"])
		inputCount = str(len(transaction["inputs"]))
		outputCount = str(len(transaction["out"]))
		lockTime = str(transaction["lock_time"])	
		hashTransaction = str(transaction["hash"])

		transactionObject = Transaction(transactionVersion,inputCount,outputCount,lockTime,hashTransaction) 
		transactions.append(transactionObject)

		for inputN in transaction["inputs"]:
			if(len(list(inputN.keys()))>2):
				indexPreviousTxout = str(hex(inputN["prev_out"]["n"])) # PENSAR SI ES MEJOR SIEMPRE EN INT(Habría que modificar guardado)
				script = str(inputN["script"])
				scriptLength = str(len(script)/2)  
				sequenceNumber = str(hex(inputN["sequence"]))
				urlPreviousTx = ''.join([urlTransaction,str(inputN["prev_out"]["tx_index"])])
				hashPreviousTransaction = str(getJSON(urlPreviousTx)["hash"])
			else:
				indexPreviousTxout = ''.join(['f']*8)
				script = str(inputN["script"])
				scriptLength = str(len(script)/2)
				sequenceNumber = str(hex(inputN["sequence"]))
				hashPreviousTransaction = ''.join(['0']*64)
			inputs.append(Input(indexPreviousTxout,scriptLength,script,sequenceNumber,hashPreviousTransaction))

		for output in transaction["out"]:
			valueSatoshis = str(output["value"])
			lockingScript = str(output["script"])
			scriptLength = str(len(lockingScript)/2) 
			indexTxOut = str(hex(output["n"])) # PENSAR SI ES MEJOR SIEMPRE EN INT(Habría que modificar guardado)
			outputs.append(Output(valueSatoshis,scriptLength,lockingScript,indexOrderOut))

	lastIndexSaved += 1
	fileIndex = open('indexBlock.txt', 'w')
	fileINdex.write(lastIndexSaved)
	fileINdex.close()

	storeNodes(newBlockToSave,transactions,inputs,outputs)

	


def main():
	lastBlockMined = getJSON(urlLastBlock)
	indexLastBlockMined = lastBlockMined["block_index"]
	print indexLastBlockMined
	global lastIndexSaved
	fileIndex = open('indexBlock.txt', 'r')
	lastIndexSaved = fileIndex.readline()
	fileIndex.close()
	print lastIndexSaved
	while(int(lastIndexSaved)<indexLastBlockMined):
		getBlock(int(lastIndexSaved)+1)
	main()


timestart = datetime.datetime.now()
main()
timefinish = datetime.datetime.now()
print "Ha tardado:", (timefinish-timestart)
