# coding=utf-8
import datetime
import time
import struct 
import threading
import hashlib
import py2neo
from py2neo import Graph, Node, Relationship

blockchain_db = Graph("http://localhost:7474/db/data/", user="neo4j", password="123456")

# ------------------------------------------------------------------------------
# Seguir las transacciones que se producen sobre una dirección determinada
# ------------------------------------------------------------------------------
#
# Para ello, se necesita acumular las distintas direcciones que agrupan inputs en una misma transacción, así como la salida de éstas
#
# A tener en cuenta:
#	Si existen muchas direcciones de entrada y muchas de salida (informarse de cuantas son muchas) probablemente se trate de un pool


# PASOS:
#	1. Empezar con un bloque almacenado
#	2. Sacar una transacción (el primer bloque solo tiene una, que es la de recompensa) y almacenar en memoria ambos objectos en dict.
#	3. Sacar un output y almacenarlo 
#	4. Seguir ese output hasta el input que tiene asignado como gasto (hay que ver que tipo de consultas neo4j nos valen) 
#	5. Guardar en memoria el input de gasto, así como la transacción y el bloque
#	6. Sacar los demas inputs de esta transacción y almacenarlos en memoria
#	7. Obtener de esta transacción los outputs y pensar cual es el gasto y cual el cambio, y almacenar el cambio
#	8. Volver a empezar con el output cambio a realizar el seguimiento

def readTx(timeStamp):
	initialBlockTx = blockchain_db.data("OPTIONAL MATCH (b:Block {timeStamp={timeStamp}})<-[:TO]-(t:Transaction) RETURN b,t,ID(t) LIMIT 1", 
				timeStamp=timeStamp)
	initialOutput = blockchain_db.data("MATCH (t:Transaction)<-[:TO]-(o:Output) WHERE ID(t)={idTx} RETURN o,ID(o)", 
				idTx=initialBlockTx[0]['ID(t)'])
	if(initialPoint == None):
		pass #   PENSAR QUE HACER
	initialBlock = initialPoint[0]['b']
	initialTx = initialPoint[0]['t']

	# Hay que determinar cual es el output de pago y cual el cambio -> En principio se considera el de cambio el último
	outputToFollow = initialOutput[0]['o'][len(initialOutput)-1]
	idOutputToFollow = initialOutput[0]['ID(o)']
	for i in range(10):
		spendTx = blockchain_db.data("MATCH (o:Output)--(i:Input)--(t:Transaction) WHERE ID(o)={idOutputToFollow} RETURN t,ID(t)",
			idOutputToFollow=idOutputToFollow)
		inputsRelated = blockchain_db.data("MATCH (t:Transaction)--(i:Input) WHERE ID(t)={idSpendTx} RETURN i",
			idSpendTx=spendTx[0]['ID(t)'])
		newOutputsToFollow = blockchain_db.data("OPTIONAL MATCH (t:{nextInput[0]['t']})--(o:Output) RETURN o")

def readTxSimple(timeStamp):


def main():
	timeStart = datetime.datetime.now()
	print timeStart

	readTx(fecha)  # ------------ Introducir fecha en terminal

	timeFinish = datetime.datetime.now()
	print 'El programa ha tardado:',(timeFinish-timeStart)