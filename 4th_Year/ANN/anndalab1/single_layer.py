import numpy as np
import matplotlib.pyplot as plt


# patterns = input matrix (d_in x n+1) + 1 for bias term
# targets = output matrix of labels (d_out x n) (d_out = 1 in this case),
# for perceptron learning, targets should be 0,1,
# for delta rule, targets should be -1 or 1

class Single_layer:

    def __init__(self, num_data_points, input_dims, output_dims, use_bias=True, learning_rate=0.001):
        if use_bias:
            self.bias = 1
        else:
            self.bias = 0
        # self.patterns = np.ones((num_data_points+bias, input_dims))
        np.random.seed(0)
        self.weights = np.random.normal(0, 1, (1, input_dims + self.bias))
        self.learning_rate = learning_rate
        self.output = np.zeros((output_dims, num_data_points))
        self.errors = []

    def forward(self, data):

        output = np.dot(self.weights, data)
        # print(output)

        # threshold
        # output = np.where(output < 0, -1, 1)
        return output

    def perceptron_rule(self, data, output, targets):
        # weight update is x if positive class is misclassified as negative,
        # -x if negative class is misclassified as positive
        # thresholding:
        output = np.where(output < 0, 0, 1)
        weight_update = np.sum((targets - output) * data, axis=1)
        self.weights += self.learning_rate * weight_update
        misclassified = np.sum((targets - output) ** 2) / len(targets)
        return misclassified

    def delta_rule(self, data, output, targets):
        # print(output)
        # print(targets)
        error = targets - output
        # print(error)
        weight_update = np.dot(error, np.transpose(data))
        self.weights += self.learning_rate * weight_update
        return np.sum(error**2)/2

    def training_perceptron_rule(self, data, targets, epochs=100):
        if self.bias == 1:
            ones = np.ones((1, data.shape[1]))
            data = np.concatenate((data, ones), axis=0)
        # epochs = 20
        d_error = 1000
        old_error = 1000
        epoch = 1
        while epoch < epochs or d_error > 0.01:
            output = self.forward(data)
            error = self.perceptron_rule(data, output, targets)
            print("training epoch " + str(epoch), "error: ", error)
            d_error = abs(old_error - error)
            old_error = error
            epoch += 1
        self.output = output

    def training_delta_rule(self, data, targets, epochs=200, batch_learning=True):
        if self.bias == 1:
            ones = np.ones((1, data.shape[1]))
            data = np.concatenate((data, ones), axis=0)
        epoch = 1
        d_err = 1000
        old_err = 1000

        if batch_learning:

            for e in range(epochs):
                output = self.forward(data)

                error = self.delta_rule(data, output, targets)
                self.errors.append(error)
                d_err = abs(old_err - error)
                old_err = error
                print("training epoch: ", epoch, error)
                epoch = epoch + 1
                if d_err <= 0.01:
                    break

        else:
            while epoch < epochs:
                tot_err = 0
                for sample in range(data.shape[1]):
                    input = np.reshape(data[:, sample], (data.shape[0], 1))
                    output = self.forward(input)
                    error = self.delta_rule(input, output, targets[sample])
                    tot_err += error ** 2
                tot_err = tot_err[0]/2
                #tot_err = tot_err[0]
                print(tot_err)
                if epoch > 1:
                    old_err = self.errors[-1]
                    d_err = abs(old_err - tot_err)
                if d_err <= 0.01:
                    break
                self.errors.append(tot_err)
                print("epoch", epoch, "error: ", tot_err)
                epoch += 1



'''n = 100
mA = [0.8, 0.5]
sigmaA = 1
mB = [-0.8, 0.0]
sigmaB = 1
classA = np.ndarray(shape=(2,n))
classB = np.ndarray(shape=(2,n))
classA[0,:] = np.random.normal(0,1,(1,n)) * sigmaA + mA[0]
classA[1,:] = np.random.normal(0,1,(1,n)) + sigmaA + mA[1]
classB[0,:] = np.random.normal(0,1,(1,n)) * sigmaB + mB[0]
classB[1,:] = np.random.normal(0,1,(1,n)) * sigmaB + mB[1]
data = np.concatenate((classA, classB), axis=1)
targets_for_perceptron = np.array([1 for i in range(n)] + [0 for i in range(n)])
targets = np.array([1 for i in range(n)] + [-1 for i in range(n)])
targets1 = targets_for_perceptron.reshape(1,-1)
data_targets = np.concatenate((data, targets1), axis=0)
np.random.shuffle(data_targets.transpose())
data = data_targets[:2,:]
targets = data_targets[2,:]'''

n = 100
mA = [1.0, 0.3]
mB = [0.0, -0.1]
sigmaA = 0.2
sigmaB = 0.3
classA = np.ndarray(shape=(2,n))
classB = np.ndarray(shape=(2, n))
classA1 = np.random.normal(0,1,(1, int(0.5*n))) * sigmaA -mA[0]
classA2 = np.random.normal(0,1,(1, int(0.5*n))) * sigmaA + mA[0]
classA[0,:] = np.concatenate((classA1, classA2), axis=1)
classA[1,:] = np.random.normal(0,1,(1,n)) * sigmaA + mA[1]
classB[0,:] = np.random.normal(0,1,(1,n)) * sigmaB + mB[0]
classB[1,:] = np.random.normal(0,1,(1,n)) * sigmaB + mB[1]
np.random.shuffle(classA)
np.random.shuffle(classB)
#subsample
classAneg = []
classApos = []
'''for a in range(classA.shape[1]):
    val = classA[:, a]
    if val[0] < 0:
        classAneg.append(val)
    elif val[0] > 0:
        classApos.append(val)
a = len(classAneg)
b = len(classApos)
classAbias_neg = np.array(classAneg[:int(0.8*len(classAneg))])
classAbias_pos = np.array(classApos[:int(0.2*len(classApos))])
classAbias = np.concatenate((classAbias_neg, classAbias_pos), axis=0)'''
classAsub = classA[:,:50]
classBsub = classB[:,:50]
print(classAsub.shape)
targets = np.array([1 for i in range(n)] + [-1 for i in range(50)])
print(targets.shape)
data = np.concatenate((classA, classBsub),axis=1)
print(data.shape)

#subsample
#data_targets = np.concatenate((data, targets.reshape(1,-1)), axis=0)

#print(data_targets.shape)


single = Single_layer(100, 2, 1)
single.training_delta_rule(data, targets)
#print(targets)
print(single.weights)
weights = single.weights
x_range = np.linspace(2,-2,200)
#y_range = -weights[:,0]/weights[:,1]*x_range
y_range = -(weights[:,2]/weights[:,1])/(weights[:,2]/weights[:,0])*x_range - weights[:,2]/weights[:,1]
plt.scatter(data[0], data[1], c=targets)
plt.axis([-2,2,-2,2])
plt.title("Delta rule batch learning, bias")

plt.plot(x_range, y_range, c="r")
plt.show()
epochs = np.arange(int(len(single.errors)))
#print(epochs)
#print(single2.errors)
#print(epochs)
plt.plot(epochs, single.errors)
plt.title("classification error batch learning")
plt.show()

