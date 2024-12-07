import numpy as np
import matplotlib.pyplot as plt

class HopfieldNetwork:
    def __init__(self):
        self.weights = 0
        self.patterns = 0
        self.targets = 0

    def sign(self, data):
        return np.where(data < 0, -1, 1)

    def transfer(self, data):
        output = np.dot(data, self.weights)
        return self.sign(output)

    def outer_product(self, one, two):
        self.weights = np.outer(one, two)
        return np.outer(one, two)

    def learn_weights(self, data, targets):
        self.patterns = data
        self.targets = targets
        self.weights = np.dot(np.transpose(self.targets), self.targets)

    def update_rule(self):
        self.patterns = self.transfer(self.patterns)
        return self.patterns


#data creation

x1 = np.array([[-1, -1, 1, -1, 1, -1, -1, 1]])
x2 = np.array([[-1, -1, -1, -1, -1, 1, -1, -1]])
x3 = np.array([[-1, 1, 1, -1, -1, 1, -1, 1]])

targets = np.concatenate((x1, x2, x3), axis=0)
targets = np.transpose(targets)
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
data = np.transpose(data)
epochs = 3
model = HopfieldNetwork()
model.learn_weights(data, targets)
print(model.patterns)
print(model.targets)


#run for 20 epochs check for convergence and number of bits wrong
counts = np.zeros((data.shape[0]))
for epoch in range(epochs):
    print("epoch " + str(epoch))
    output = model.update_rule()
    print("output pattern")
    print(output)
    print("pattern from model")
    print(model.patterns)
    print("target pattern")
    print(model.targets)

    for x in range(output.shape[1]):
        point = output[:,x]
        target = targets[:,x]

        #if point.all() == target.all():
         #   print("pattern " + str(x) + " learned")
        #else:
        wrong = np.where(point == target, 0, 1)
        count = np.sum(wrong)
        print("pattern " + str(x) + " " + str(count) + " bits wrong")
        if count > 0:
            print(str(count) + "bits wrong in pattern " + str(x))
        counts[x] = count
    all_counts = np.sum(counts)
    if all_counts == 0:
        break
    #if output.all() == targets.all():
     #   print("all learned")
      #  print("patterns ", output)
       # print("targets", targets)
        #break


#more dissimilar starting pattern:
#targets
x1 = np.array([[-1, -1, 1, -1, 1, -1, -1, 1]])
x2 = np.array([[-1, -1, -1, -1, -1, 1, -1, -1]])
x3 = np.array([[-1, 1, 1, -1, -1, 1, -1, 1]])
#data with 12 wrong bits, 4 in each
x1d = np.array([[1, -1, -1, -1, -1, 1, -1, 1]])
x2d = np.array([[1, 1, -1, 1, -1, 1, -1, 1]])
x3d = np.array([[1, 1, -1, -1, 1, -1, -1, 1]])

data = np.concatenate((x1d, x2d, x3d), axis=0)

#run for 20 epochs check for convergence and number of bits wrong
model.learn_weights(data, targets)
print("more wrong data")
for epoch in range(epochs):
    print("epoch " + str(epoch))
    output = model.update_rule()
    for x in range(output.shape[0]):
        if output[x,:].all() == targets[x,:].all():
            print("pattern " + str(x) + " learned")
        else:
            count = np.where(output[x,:] == targets[x,:], 0, 1)
            print("pattern " + str(x) + str(count) + "bits wrong")
    if output.all() == targets.all():
        print("all learned")
        print("patterns ", output)
        print("targets", targets)
        break

hello =2

