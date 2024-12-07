import torch
#from torch import nn
import numpy as np

def gen_data():
    #generate a mackey-glass time series from x(0) = 1.5
    x = np.array([1.5])
    for t in range(1, 1505):
        if t < 25:
            value = x[t-1] - 0.1*x[t-1]
            x.append(value)
        value = x[t-1] + (0.2*x[t-25])/(1+x[t-25]**10) - 0.1*x[t-1]
        x.append(value)
    input = torch.tensor([x[301-20], x[301-15], x[301-10], x[301-5], x[301]])
    output = torch.tensor([x[301+5]])
    for t in range(302, 1500):
        next = torch.tensor([x[t-20], x[t-15], x[t-10], x[t-5], x[t]])
        nextout = torch.tensor(x[t+5])
        input = torch.cat((input, next), 1)
        output = torch.cat((output, nextout), 1)
    return input, output

class MLP(torch.nn.Module):

    # defines a neural network, number of hidden layers and nodes parameterized
    # as a list of integers where the number represents the number of nodes in that layer
    def __init__(self, hidden_sizes, input_dims=5, output_dims=5):
        super(MLP, self).__init__()
        self.input = torch.nn.Linear(input_dims, hidden_sizes[0])

        self.hidden = torch.nn.Sequential()
        for k in range(len(hidden_sizes-1)):
            self.hidden.add_module("hidden"+str(k), torch.nn.Linear(hidden_sizes[k], hidden_sizes[k+1]))
            self.hidden.add_module("sig" + str(k), torch.nn.Sigmoid())

        self.out = torch.nn.Linear(hidden_sizes[-1], output_dims)

    def forward(self, data): #TODO do we need this? torch has a built in forward() method
        input = self.input(data)
        hidden = self.hidden(input)
        output = self.out(hidden)

    #TODO backprop w regularization, early stopping

    #TODO


