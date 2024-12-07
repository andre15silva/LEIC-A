import numpy as np
import matplotlib.pyplot as plt
import itertools

class HopfieldNetwork:
    def __init__(self):
        self.weights = 0
        self.pattern = 0
        self.targets = 0

    def sign(self, data):
        return np.where(data >= 0, 1, -1)

    def transfer(self, data):
        output = np.dot(self.weights, data)
        return self.sign(output)

    def outer_product(self, one, two):
        self.weights = np.outer(one, two)
        return np.outer(one, two)

    def learn_weights(self, targets):
        #self.pattern = data
        self.targets = targets
        #self.weights = np.dot(self.targets, np.transpose(self.targets))
        self.weights = np.dot(targets.transpose(), targets)

    def set_pattern(self, pattern):
        self.pattern = pattern

    def update_rule(self):
        self.pattern = self.transfer(self.pattern)
        #output = self.transfer(pattern)
        return #output

    def update_rule_async(self):
        j = np.random.randint(0, self.pattern.shape[0])
        self.pattern[j] = self.sign(np.dot(self.weights[j,:], self.pattern))

    def is_fixed_point(self, x):
        self.pattern = x
        self.update_rule()
        if np.all(x == self.pattern):
            return True
        else:
            return False

    def energy(self):
        output = 0
        for i in range(self.pattern.shape[0]):
            for j in range(self.pattern.shape[0]):
                output += self.weights[i,j] * self.pattern[i] * self.pattern[j]
        return -output

def testing_loop(model, data, targets, epochs=10):
    model.learn_weights(targets)
    # print(model.weights)
    # for i in range(data.shape[1]):
    # model.weights[i,i] = 0
    # print(model.weights)
    for x in range(data.shape[0]):
        vector = data[x, :]
        target = targets[x, :]
        model.set_pattern(vector)
        print("pattern " + str(x))
        # print("pattern: ", model.pattern)
        # print("target: ", target)
        for epoch in range(epochs):
            print("epoch: " + str(epoch))
            # wrong = np.where(model.pattern != target)[0]
            # print("wrong at indices; ", wrong)
            # print("before update")
            # print(model.pattern)
            model.update_rule()
            # print("after update")
            # print(model.pattern)
            # print("target")
            # print(target)
            # wrong = np.where(model.pattern != target)[0]
            # print("wrong at indices; ", wrong)
            # count = wrong.size
            if np.all(model.pattern == target):
                # print("wrong bits: " + str(count))
                print("pattern learned")
                break
            else:
                continue
                # print("wrong bits: " + str(count))
                # vector = output

def find_attractors(model):
    #find all attractors
    all_possible = list(itertools.product([-1, 1], repeat=8))
    all_attractors = []
    for x in all_possible:
        vector = np.array(x)
        if model.is_fixed_point(vector):
            all_attractors.append(vector)

    print("Number of candidates: ", len(all_possible))
    num_attractors = len(all_attractors)
    print("Number of attractors: ", num_attractors)

#data creation

x1 = np.array([[-1, -1, 1, -1, 1, -1, -1, 1]])
x2 = np.array([[-1, -1, -1, -1, -1, 1, -1, -1]])
x3 = np.array([[-1, 1, 1, -1, -1, 1, -1, 1]])

targets = np.concatenate((x1, x2, x3), axis=0)
#targets = np.transpose(targets)
'''
#measuring correlations
model = HopfieldNetwork()
output = np.array((data.shape))
for x in data:
    model.outer_product(np.transpose(x), x)
    out = model.transfer(x)
    print(out)
    output[x] = out
if output.all() == targets.all():
    print("true")

#2.2
model.learn_weights(targets)
output = model.transfer(targets)
print(output)
if output.all() == targets.all():
    print("True")'''

#3.1 original data with one or two bit errors
x1d = np.array([[1, -1, 1, -1, 1, -1, -1, 1]])
x2d = np.array([[1, 1, -1, -1, -1, 1, -1, -1]])
x3d = np.array([[1, 1, 1, -1, 1, 1, -1, 1]])

data = np.concatenate((x1d, x2d, x3d), axis=0)
#data in columns
#data = np.transpose(data)

#model = HopfieldNetwork()


#more dissimilar starting pattern:
#targets
x1 = np.array([[-1, -1, 1, -1, 1, -1, -1, 1]])
x2 = np.array([[-1, -1, -1, -1, -1, 1, -1, -1]])
x3 = np.array([[-1, 1, 1, -1, -1, 1, -1, 1]])
#data with 5 wrong bits each
x1d = np.array([[1, 1, 1, -1, 1, -1, -1, 1]])
x2d = np.array([[1, 1, -1, -1, -1, 1, -1, -1]])
x3d = np.array([[1, -1, 1, -1, -1, 1, -1, 1]])

targets = np.concatenate((x1,x2,x3), axis=0)
data = np.concatenate((x1d, x2d, x3d), axis=0)

#testing_loop(model, data, targets, epochs=50)



