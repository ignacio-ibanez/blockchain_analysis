# coding=utf-8
import datetime
import time
import struct 
import threading
import hashlib
import py2neo
from py2neo import Graph, Node, Relationship


class Block(object):       
	def __init__(self,magicID,blockSize,blockHeader,transactionsCount,hashHeader,timeStamp,version):
		self.magicID = magicID
		self.blockSize = blockSize
		self.blockHeader = blockHeader
		self.transactionsCount = transactionsCount
		self.hashHeader = hashHeader
		self.timeStamp = timeStamp
		self.version = version


class Header(object):
	def __init__(self,version,previousBlockHash,merkleRoot,timeStamp,difficultyTarget,nonce):
		self.version = version
		self.previousBlockHash = previousBlockHash
		self.merkleRoot = merkleRoot
		self.timeStamp = timeStamp
		self.difficultyTarget = difficultyTarget
		self.nonce = nonce

#4104f0d742c83392ae7f885fca7739a1681df9039d9930f4eede9fef7b7c85fb191fad43e584345e8b872c84260b39c2e192512ead631083be327a6825fa0ba561e5ac
#4104f0d742c83392ae7f885fca7739a1681df9039d9930f4eede9fef7b7c85fb191fad43e
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
	def __init__(self,valueSatoshis,scriptPublicKey,indexTxOut):
		self.valueSatoshis = valueSatoshis
		self.scriptPublicKey = scriptPublicKey
		self.indexTxOut = indexTxOut



lastBlockRead = 0
lineBeginBlock = ''
nextLineBeginBlock = ''
blockChainList = []
blockChainRead = False
timeStart = datetime.datetime.now()

previousTimeStamp = int("49696ef8",16)
timeStamp3minutes = 180

flag = threading.Event()

blockchain_db = Graph("http://localhost:7474/db/data/", user="neo4j", password="123456")
blockchain_db.run("CREATE INDEX ON :Block(hashHeader)")
blockchain_db.run("CREATE INDEX ON :Transaction(hashTransaction)")
#blockchain_db.schema.create_index(Block,hashHeaderReduced)
#blockchain_db.schema.create_index(Transaction,hashTransactionReduced)

newBlockToSave = None
previousChainBlock = None

SecondNodeHashKept = ''
SecondNodeHashCalculated = ''

blocksRead = 0


blockPack = []
blockInPack = 0
blockOrphan = 0
hashFirstBlockPack = ''

startStoring = False

lastSixBlocks = []

def readBlockchain(): 
	#f = open('blockchainReduced.dat','r')
	#blockchainFile = open('bootstrap.dat','r')
	# pruebaAmp.dat -> 20.000 primeras lineas
	with open('blockchainReduced.dat','rb') as blockchainFile:  
		#print 'Prueba de magicId: ', ord(blockchainFile.read(1))
		#linea = blockchainFile.readline()
		#print 'Prueba de primera linea: ', linea
		#print 'Es String?: ', isinstance(magicId,str)

		global lastBlockRead
		global lineBeginBlock
		global nextLineBeginBlock
		global blockChainRead

			
		workingLine = ''
		workingBlock = []
		isBlockBeginning = True
		block = ''
		isNewBlock = True
	
		while(True):
			if(isNewBlock and lineBeginBlock!=''):
				workingBlock = []
				workingBlock.append(lineBeginBlock)
				if(nextLineBeginBlock !=''):
					workingBlock.append(nextLineBeginBlock)
				nextLineBeginBlock = ''
				isNewBlock = False
				block = ''
			
			previousLineRead = workingLine
			workingLine = blockchainFile.readline().replace(' ', '').rstrip()
			workingBlock.append(workingLine)


			if(previousLineRead == '' and workingLine == ''):
				blockchainFile.close()
				break
			
			if(checkEndBlock(workingLine, previousLineRead)):
				if(isBlockBeginning):
					isBlockBeginning = False
				else:
					lastBlockRead = lastBlockRead + 1
					for line in workingBlock:
						block = ''.join([block,line])
					isNewBlock = True
					# Elimina la parte del bloque siguiente que se ha quedado en la última línea
					block = cleanBlock(block) 
					blockChainList.append(block)

					if(flag.isSet()):
						flag.wait()

		blockChainRead = True


# Encuentra el final de cada bloque
def checkEndBlock(actual , previous): 
	global lineBeginBlock
	global nextLineBeginBlock
	if(actual.startswith('f9beb4d9') or actual.startswith('d9b4bef9')):
		lineBeginBlock = actual
		return True
	elif(actual.count('f9beb4d9')!=0 or actual.count('d9b4bef9')!=0):
		lineBeginBlock = actual
		return True
	elif(actual.startswith('b4d9') and previous.endswith('f9be')):
		lineBeginBlock = previous
		nextLineBeginBlock = actual
		return True
	elif(actual.startswith('beb4d9') and previous.endswith('f9')):
		lineBeginBlock = previous
		nextLineBeginBlock = actual
		return True
	elif(actual.startswith('d9') and previous.endswith('f9beb4')):
		lineBeginBlock = previous
		nextLineBeginBlock = actual
		return True
	elif(actual.startswith('bef9') and previous.endswith('d9b4')):
		lineBeginBlock = previous
		nextLineBeginBlock = actual
		return True
	else:
		return False


def cleanBlock(block):
	if(block.count('f9beb4d9') > 1):
		indexStart = block.index('f9beb4d9')
		indexEnd = block.index('f9beb4d9', indexStart+5)
		block = block[indexStart:indexEnd]
	return block


# Obtiene los distintos campos de cada bloque
def getBlockContent(block):
	
	transactions = []
	transactionsInputs = []
	transactionsOutputs = []
	
	global newBlockToSave
	global previousChainBlock
	global blockPack
	global blockInPack 
	global blockOrphan 
	global hashFirstBlockPack
	global lastSixBlocks
	global startStoring
	global timeStamp3minits
	global previousTimeStamp

	#if(blocksRead == 29664 or blocksRead == 39317):
	#	return

	magicID = endianness(block[:8])
	blockSize = endianness(block[8:16])

	# CABECERA DEL BLOQUE
	blockHeader = block[16:176]
	version = endianness(block[16:24])
	previousBlockHash = endianness(block[24:88])
	#merkleRoot = endianness(block[88:152])
	timeStamp = endianness(block[152:160])
	timeStampInt = int(timeStamp,16)
	#timeStampDecoded = time.gmtime(int(timeStamp,16))
	#timeStampFormat = time.strftime('%d-%m-%Y %H:%M:%S', timeStampDecoded)
	#difficultyTarget = endianness(block[160:168])
	#nonce = endianness(block[168:176])

	# SIRVE PARA CORREGIR ERRORES DE REPETICION DEL BLOCKCHAIN
	#if(timeStamp == "49696ef8" && blocksRead != 0):
	#	startStoring = True
	#if(startStoring == False):
	#	return 

	if(timeStampInt < (previousTimeStamp-timeStamp3minutes)):
		return
	else:
		previousTimeStamp = int(timeStamp,16)

	# SIRVE PARA COMPROBAR SI ESE BLOQUE HA SIDO GUARDADO POR LA PARTE DE REAL-TIME
	if(timeStampInt > (1484233949+timeStamp3minutes)):
		return


	headerBlockHex = block[16:176].decode('hex')
	hashHeader = hashlib.sha256(hashlib.sha256(headerBlockHex).digest()).digest().encode('hex_codec')
	hashHeader = endianness(hashHeader)


	# CONTADOR DE TRANSACCIONES
	variableLenghtTransactions = getVariableLength(block[176:194])
	transactionsCount = int(endianness(block[176:176+variableLenghtTransactions*2]),16)
	
	newBlockToSave = Block(magicID,blockSize,blockHeader,transactionsCount,hashHeader,timeStamp,version)
	
	# TRANSACCIONES
	indexBeginTransaction = 176+variableLenghtTransactions*2 
	transactionsSaved = 0
	while(transactionsSaved < transactionsCount):
		transactionsSaved += 1
		inputsSaved = 0
		outputsSaved = 0
		inputs = []
		outputs = []

		transactionVersion = endianness(block[indexBeginTransaction:indexBeginTransaction+8])

		# INPUTS
		variableLenghtInputs = getVariableLength(block[indexBeginTransaction+8:indexBeginTransaction+8+18])
		inputCount = int(endianness(block[indexBeginTransaction+8:indexBeginTransaction+8+variableLenghtInputs*2]),16)
		indexFirstInput = indexBeginTransaction+8+variableLenghtInputs*2
		# La primera transaccion siempre tendrá el campo hash de la transacción anterior todo ceros, ya que es la recompensa al minero
		indexesFromFirstInput = 0
		while(inputsSaved < inputCount):
			hashPreviousTransaction = endianness(block[indexFirstInput+indexesFromFirstInput:indexFirstInput+indexesFromFirstInput+64])
			hashPreviousTransactionReduced = hashPreviousTransaction[57::]
			indexesFromFirstInput += 64

			indexPreviousTxout = endianness(block[indexFirstInput+indexesFromFirstInput:indexFirstInput+indexesFromFirstInput+8])
			indexesFromFirstInput += 8

			variableLenghtTxinScript = getVariableLength(block[indexFirstInput+indexesFromFirstInput:indexFirstInput+indexesFromFirstInput+18])
			if(variableLenghtTxinScript == None):
				print blocksRead, datetime.datetime.now()
				tx = blockchain_db.begin() # FALTA CREAR LA RELACION CON EL BLOQUE ANTERIOR
				newBlockNode = Node("Block", magicId=newBlockToSave.magicID, blockSize=newBlockToSave.blockSize, 
					blockHeader=newBlockToSave.blockHeader, transactionsCount=newBlockToSave.transactionsCount, 
					hashHeader=newBlockToSave.hashHeader,
					timeStamp=newBlockToSave.timeStamp, version=newBlockToSave.version)
				tx.create(newBlockNode)
				tx.commit()
				return
			scriptLength = int(endianness(block[indexFirstInput+indexesFromFirstInput:indexFirstInput+indexesFromFirstInput+variableLenghtTxinScript*2]),16)
			indexesFromFirstInput += variableLenghtTxinScript*2

			script = endianness(block[indexFirstInput+indexesFromFirstInput:indexFirstInput+indexesFromFirstInput+scriptLength*2])
			indexesFromFirstInput += scriptLength*2

			sequenceNumber = endianness(block[indexFirstInput+indexesFromFirstInput:indexFirstInput+indexesFromFirstInput+8])
			indexesFromFirstInput += 8

			inputs.append(Input(indexPreviousTxout,scriptLength,script,sequenceNumber,hashPreviousTransactionReduced))
			inputsSaved += 1 


		# OUTPUTS
		variableLenghtOutputs = getVariableLength(block[indexFirstInput+indexesFromFirstInput:indexFirstInput+indexesFromFirstInput+18])
		outputCount = int(endianness(block[indexFirstInput+indexesFromFirstInput:indexFirstInput+indexesFromFirstInput+variableLenghtOutputs*2]),16)
	
		indexFirstOutput = indexFirstInput+indexesFromFirstInput+variableLenghtOutputs*2
		indexesFromFirstOutput = 0 
		while(outputsSaved < outputCount):
			valueSatoshis = int(endianness(block[indexFirstOutput+indexesFromFirstOutput:indexFirstOutput+indexesFromFirstOutput+16]),16)
			valueBitcoins = valueSatoshis * (10**(-8))
			indexesFromFirstOutput += 16

			variableLengthOutputScript = getVariableLength(block[indexFirstOutput+indexesFromFirstOutput:indexFirstOutput+indexesFromFirstOutput+18])
			if(variableLengthOutputScript == None):
				print blocksRead, datetime.datetime.now()
				tx = blockchain_db.begin()
				newBlockNode = Node("Block", magicId=newBlockToSave.magicID, blockSize=newBlockToSave.blockSize, 
					blockHeader=newBlockToSave.blockHeader, transactionsCount=newBlockToSave.transactionsCount, 
					hashHeader=newBlockToSave.hashHeader, 
					timeStamp=newBlockToSave.timeStamp, version=newBlockToSave.version)
				tx.create(newBlockNode)
				tx.commit()
				return
			scriptLength = int(endianness(block[indexFirstOutput+indexesFromFirstOutput:indexFirstOutput+indexesFromFirstOutput+variableLengthOutputScript*2]),16)
			indexesFromFirstOutput += variableLengthOutputScript*2


			# SIN ENDIANNESS
			scriptPublicKey = block[indexFirstOutput+indexesFromFirstOutput:indexFirstOutput+indexesFromFirstOutput+scriptLength*2]
			scriptPublicKey = getScriptPublicKey(scriptPublicKey)
			indexesFromFirstOutput += scriptLength*2

			indexOrderOut = ''.join(['0'*(8-len(str(hex(outputsSaved))[2:])),str(hex(outputsSaved))[2:]])

			outputs.append(Output(valueSatoshis,scriptPublicKey,indexOrderOut))
			outputsSaved += 1


		lockTime = endianness(block[indexFirstOutput+indexesFromFirstOutput:indexFirstOutput+indexesFromFirstOutput+8])

		transactionToHash = block[indexBeginTransaction:indexFirstOutput+indexesFromFirstOutput+8].decode('hex')
		hashTransaction = hashlib.sha256(hashlib.sha256(transactionToHash).digest()).digest().encode('hex_codec')
		hashTransaction = endianness(hashTransaction)
		hashTransactionReduced = hashTransaction[57::]

		indexBeginTransaction = indexFirstOutput+indexesFromFirstOutput+8

		transactionObject = Transaction(transactionVersion,inputCount,outputCount,lockTime,hashTransaction,hashTransactionReduced) 
		transactions.append(transactionObject)

		transactionsInputs.append(inputs)
		transactionsOutputs.append(outputs)




	# ------------------------------------
	# Guardado del bloque leido
	# ------------------------------------
	tx = blockchain_db.begin()

	newBlockNode = Node("Block", magicId=newBlockToSave.magicID, blockSize=newBlockToSave.blockSize, 
					blockHeader=newBlockToSave.blockHeader, transactionsCount=newBlockToSave.transactionsCount, 
					hashHeader=newBlockToSave.hashHeader, 
					timeStamp=newBlockToSave.timeStamp, version=newBlockToSave.version)
	tx.create(newBlockNode)

	lastSixBlocks.append(newBlockNode)

	transactionsSavedNeo4j = 0
	iTx = 0
	while(transactionsSavedNeo4j < transactionsCount):
		transactionNode = Node("Transaction", transactionVersion=transactions[iTx].transactionVersion, 
					inputCount=transactions[iTx].inputCount, outputCount=transactions[iTx].outputCount, 
					lockTime=transactions[iTx].lockTime, hashTransaction=transactions[iTx].hashTransaction,
					hashTransactionReduced=transactions[iTx].hashTransactionReduced)
		tx.create(transactionNode)

		inputsWithoutOrigin = []
		inputsNodesWithoutOrigin = []
		for inputObj in transactionsInputs[iTx]:
			inputNode = Node("Input", indexPreviousTxout=inputObj.indexPreviousTxout, scriptLength=inputObj.scriptLength,
					script=inputObj.script, sequenceNumber=inputObj.sequenceNumber,
					hashPreviousTransactionReduced=inputObj.hashPreviousTransactionReduced)
			tx.create(inputNode)
			tx.create(Relationship(inputNode, transactionNode))
			# --------------------------------------------------
			# Busco el nodo output que hace refencia al origen del input
			# --------------------------------------------------
			if(inputObj.indexPreviousTxout != "ffffffff"):
				previousOutput = blockchain_db.data("OPTIONAL MATCH (t:Transaction {hashTransactionReduced: {hashPreviousTransactionReduced}})<-[:TO]-(out:Output) WHERE out.indexTxOut={indexPreviousTxout} RETURN out", 
							hashPreviousTransactionReduced=inputObj.hashPreviousTransactionReduced, 
							indexPreviousTxout=inputObj.indexPreviousTxout)
				previousOutputNode = previousOutput[0]['out']
				if (previousOutputNode != None):
					tx.create(Relationship(inputNode,'ORIGIN_OUTPUT',previousOutputNode))
				else:
					inputsWithoutOrigin.append(inputObj)
					inputsNodesWithoutOrigin.append(inputNode)

		for outputObj in transactionsOutputs[iTx]:
			outputNode = Node("Output", valueSatoshis=outputObj.valueSatoshis, 
					scriptPublicKey=outputObj.scriptPublicKey, indexTxOut=outputObj.indexTxOut)
			#tx.create(outputNode)
			tx.create(Relationship(outputNode, transactionNode))

		tx.create(Relationship(transactionNode, newBlockNode))

		transactionsSavedNeo4j += 1
		iTx += 1

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

	# ----------------------------------------------------------------
	# Buscamos el bloque anterior para crear la relación entre bloques
	# ----------------------------------------------------------------
	if(previousBlockHash != ''.join(['0']*64)):
		previousChainBlock = None
		# Bloque anterior
		if(len(lastSixBlocks)>0):
			for block in lastSixBlocks[::-1]:
				if(block['hashHeader'] == previousBlockHash):
					previousChainBlock = block
					break
			if(previousChainBlock == None): 
				previousChainBlock = blockchain_db.find_one('Block', 
					property_key='hashHeader', property_value=previousBlockHash)
		else:
			previousChainBlock = blockchain_db.find_one('Block', 
				property_key='hashHeader', property_value=previousBlockHash)

		if(previousChainBlock != None):
			prevBlockRelation = Relationship(newBlockNode,'PREVIOUS_BLOCK',previousChainBlock)
			blockchain_db.create(prevBlockRelation)
		if(len(lastSixBlocks)==6):
			lastSixBlocks.remove(lastSixBlocks[0])
	

def getScriptPublicKey(lockingScript):
	lenScript = len(lockingScript);
	if(lenScript == 134):
		scriptPublicKey = lockingScript[2:132]
	elif(lenScript == 132):
		scriptPublicKey = lockingScript[:132]
	elif(lenScript == 10):
		return '0'
	else:
		if('76a9' in lockingScript):
			indexStart = lockingScript.index('76a9')
			scriptPublicKey = lockingScript[indexStart+6:indexStart+46]
		elif('a914' in lockingScript):
			indexStart = lockingScript.index('a914')
			scriptPublicKey = lockingScript[indexStart+4:indexStart+44]
		else:
			return '0'

	return scriptPublicKey



def getVariableLength(field):
	if(field == ''):
		return None
	if(int(field[0:2],16) < int('0xfd',16)):
		return 1
	elif(int(field[0:5],16) < int('0xffff',16)):
		return 3
	elif(int(field[0:9],16) < int('0xffffffff',16)):
		return 5
	else:
		return 9
	

def endianness(field):
	bytesField = len(field)/2
	fieldConverted = ''
	if(bytesField <= 2):
		return field
	else:
		for i in range(bytesField):
			iAux = 2*i
			fieldConverted = ''.join([field[iAux:iAux+2],fieldConverted])
		return fieldConverted



def saveBlockchain():
	global timeStart
	global blocksRead 
	while(not blockChainRead):
		while(len(blockChainList)>0):
			if(len(blockChainList) > 10):
				flag.set()
			elif(len(blockChainList) < 2):
				flag.clear()
			getBlockContent(blockChainList[0])
			blockChainList.remove(blockChainList[0])
			blocksRead += 1
			if((blocksRead%10000)==0):
				print 'nuevos 10.000:', blocksRead, datetime.datetime.now()

	timeFinish = datetime.datetime.now()
	print 'El programa ha tardado:',(timeFinish-timeStart)
	print lastBlockRead


def main():
	print timeStart
	readBlocksThread = threading.Thread(target=readBlockchain, name='ReadBlockchain')
	readBlocksThread.start()
	saveBlockchain()


main()
