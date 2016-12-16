# coding=utf-8
import json
import requests
import datetime


urlLastBlock = "https://blockchain.info/es/latestblock"
urlBlock = "https://blockchain.info/es/rawblock/"
urlTransaction = "https://blockchain.info/es/rawtx/"

def getJSON(url):
	r = requests.get(url=url)
	return r.json()


def main():
	lastBlock = getJSON(urlLastBlock)
	blockIndex = str(lastBlock["block_index"])
	urlBlockToSave = urlBlock+blockIndex
	blockToSave = getJSON(urlBlockToSave)

	# Comprobar con el bloque genesis que todos los datos están en orden
	# Extracción de los datos
	magicID = 'd9b4bef9'
	blocksize = str(blockToSave["size"])
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
	blockHeader = version+previousBlockHash+merkleRoot+timeStamp+nonce

	for transaction in blockToSave["tx"]:
		transactionVersion = str(transaction["ver"])
		inputCount = str(len(transaction["inputs"]))
		outputCount = str(len(transaction["out"]))
		lockTime = str(transaction["lock_time"])	
		hashTransaction = str(transaction["hash"])

		for inputN in transaction["inputs"]:
			if(len(list(inputN.keys()))>2):
				indexPreviousTxout = str(hex(inputN["prev_out"]["n"])) # PENSAR SI ES MEJOR SIEMPRE EN INT(Habría que modificar guardado)
				script = str(inputN["script"])
				scriptLength = str(len(script)/2)  
				sequenceNumber = str(hex(inputN["sequence"]))
				urlPreviousTx = urlTransaction+str(inputN["prev_out"]["tx_index"])
				hashPreviousTransaction = str(getJSON(urlTransaction+str(inputN["prev_out"]["tx_index"]))["hash"])
			else:
				indexPreviousTxout = ''.join(['f']*8)
				script = str(inputN["script"])
				scriptLength = str(len(script)/2)
				sequenceNumber = str(hex(inputN["sequence"]))
				hashPreviousTransaction = ''.join(['0']*64)

		for output in transaction["out"]:
			valueSatoshis = str(output["value"])
			lockingScript = str(output["script"])
			scriptLength = str(len(lockingScript)/2) 
			indexTxOut = str(hex(output["n"])) # PENSAR SI ES MEJOR SIEMPRE EN INT(Habría que modificar guardado)

timestart = datetime.datetime.now()
main()
timefinish = datetime.datetime.now()
print "Ha tardado:", (timefinish-timestart)
