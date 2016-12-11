# coding=utf-8
import json
import requests


urlLastBlock = "https://blockchain.info/es/latestblock"
urlBlock = "https://blockchain.info/es/rawblock/"

r = requests.get(url=urlLastBlock)
lastBlock = r.json()

blockIndex = str(lastBlock["block_index"])

urlBlockToSave = urlBlock+blockIndex

r = requests.get(url=urlBlockToSave)
blockToSave = r.json()

