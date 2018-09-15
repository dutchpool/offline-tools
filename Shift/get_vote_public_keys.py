import json

public_key_list = {}
votes = json.load(open("oxy_votes.json", 'r'))
delegates = json.load(open(votes["delegates_file"], 'r'))
delegates_already_in_list = []
for index, vote_to_remove in enumerate(votes["votes_to_remove"]):
    if vote_to_remove in delegates_already_in_list:
        print(vote_to_remove + " already added to the list")
    elif vote_to_remove in delegates:
        if "transaction_" + str(int(index / votes["votes_per_transaction"])) not in public_key_list:
            public_key_list["transaction_" + str(int(index / votes["votes_per_transaction"]))] = ""
        public_key_list["transaction_" + str(int(index / votes["votes_per_transaction"]))] += ",-" + delegates[vote_to_remove]
        delegates_already_in_list.append(vote_to_remove)
    else:
        print("public key for " + vote_to_remove + " can not be found")
number_of_votes_to_remove = len(votes["votes_to_remove"])
for index, vote_to_add in enumerate(votes["votes_to_add"]):
    if vote_to_add in delegates_already_in_list:
        print(vote_to_add + " already added to the list")
    elif vote_to_add in delegates:
        if "transaction_" + str(int((index + number_of_votes_to_remove) / votes["votes_per_transaction"])) not in public_key_list:
            public_key_list["transaction_" + str(int((index + number_of_votes_to_remove) / votes["votes_per_transaction"]))] = ""
        public_key_list["transaction_" + str(int((index + number_of_votes_to_remove) / votes["votes_per_transaction"]))] += ",+" + delegates[vote_to_add]
        delegates_already_in_list.append(vote_to_add)
    else:
        print("public key for " + vote_to_add + " can not be found")

for key, transaction in public_key_list.items():
    public_key_list[key] = transaction[1:]
json.dump(public_key_list, open(votes["crypto_type"] + "_public_keys_to_vote.json", 'w'), indent=4, separators=(',', ': '))

