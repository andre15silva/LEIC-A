import torch
from torch import nn
import numpy as np


class GRUCellV2(nn.Module):
    """
    GRU cell implementation
    """
    def __init__(self, input_size, hidden_size, activation=torch.tanh):
        """
        Initializes a GRU cell

        :param      input_size:      The size of the input layer
        :type       input_size:      int
        :param      hidden_size:     The size of the hidden layer
        :type       hidden_size:     int
        :param      activation:      The activation function for a new gate
        :type       activation:      callable
        """
        super(GRUCellV2, self).__init__()
        self.activation = activation

        # initialize weights by sampling from a uniform distribution between -K and K
        K = 1 / np.sqrt(hidden_size)
        # weights
        self.w_ih = nn.Parameter(torch.rand(3 * hidden_size, input_size) * 2 * K - K)
        self.w_hh = nn.Parameter(torch.rand(3 * hidden_size, hidden_size) * 2 * K - K)
        self.b_ih = nn.Parameter(torch.rand(3 * hidden_size) * 2 * K - K)
        self.b_hh = nn.Parameter(torch.rand(3 * hidden_size) * 2 * K - K)

        
    def forward(self, x, h):
        """
        Performs a forward pass through a GRU cell


        Returns the current hidden state h_t for every datapoint in batch.
        
        :param      x:    an element x_t in a sequence
        :type       x:    torch.Tensor
        :param      h:    previous hidden state h_{t-1}
        :type       h:    torch.Tensor
        """
        # input -> hidden weights (to be multiplied by the current input)
        # 60x10 -> 5x60 -> 3 * 5x20
        w_ir, w_iz, w_in = torch.chunk(torch.mm(x, self.w_ih.T), 3, 1)
        # 60x1 -> 1x60 -> 5x60 -> 3 * 5x20 // h.shape[0] is the batch_size
        b_ir, b_iz, b_in = torch.chunk(self.b_ih.T.repeat(h.shape[0], 1), 3, 1)

        # hidden -> hidden weights (to be multiplied by the previous hidden state)
        # 60x60 -> 5x60 -> 3 * 5x20
        w_hr, w_hz, w_hn = torch.chunk(torch.mm(h, self.w_hh.T), 3, 1)
        # 60x1 -> 1x60 -> 5x60 -> 3 * 5x20
        b_hr, b_hz, b_hn = torch.chunk(self.b_hh.T.repeat(h.shape[0], 1), 3, 1)

        # reset gate
        r = torch.sigmoid(w_ir + b_ir + w_hr + b_hr)

        # update gate
        z = torch.sigmoid(w_iz + b_iz + w_hz + b_hz)

        # tentative new hidden state
        n = self.activation(w_in + r * (w_hn + b_hn) + b_in)

        # new hidden state 
        # uses update gate to decide how much to keep from current and previous states
        return (1 - z) * n + z * h




class GRU2(nn.Module):
    """
    GRU network implementation
    """
    def __init__(self, input_size, hidden_size, bias=True, activation=torch.tanh, bidirectional=False):
        super(GRU2, self).__init__()
        self.bidirectional = bidirectional
        self.fw = GRUCellV2(input_size, hidden_size, activation=activation) # forward cell
        if self.bidirectional:
            self.bw = GRUCellV2(input_size, hidden_size, activation=activation) # backward cell
        self.hidden_size = hidden_size
        
    def forward(self, x):
        """
        Performs a forward pass through the whole GRU network, consisting of a number of GRU cells.
        Takes as input a 3D tensor `x` of dimensionality (B, T, D),
        where B is the batch size;
              T is the sequence length (if sequences have different lengths, they should be padded before being inputted to forward)
              D is the dimensionality of each element in the sequence, e.g. word vector dimensionality

        The method returns a 3-tuple of (outputs, h_fw, h_bw), if self.bidirectional is True,
                           a 2-tuple of (outputs, h_fw), otherwise
        `outputs` is a tensor containing the output features h_t for each t in each sequence (the same as in PyTorch native GRU class);
                  NOTE: if bidirectional is True, then it should contain a concatenation of hidden states of forward and backward cells for each sequence element.
        `h_fw` is the last hidden state of the forward cell for each sequence, i.e. when t = length of the sequence;
        `h_bw` is the last hidden state of the backward cell for each sequence, i.e. when t = 0 (because the backward cell processes a sequence backwards)
        
        :param      x:    a batch of sequences of dimensionality (B, T, D)
        :type       x:    torch.Tensor
        """

        B = x.shape[0] # batch size = 5
        T = x.shape[1] # sequence size = 3
        D = x.shape[2] # element size = 10

        # forward pass for each position in the sequences
        outputs = torch.zeros((B, T, self.hidden_size)) # h_t for each t
        previous = torch.zeros((B, self.hidden_size)) # h_t-1, zero for the first
        for t in range(T):
            outputs[:,t,:] = self.fw.forward(x[:,t,:], previous)
            previous = outputs[:,t,:].clone()

        # backwads pass for each position in the sequences, reverse order now
        if self.bidirectional:
            outputs_back = torch.zeros((B, T, self.hidden_size)) # h_t for each t but backwards
            previous = torch.zeros((B, self.hidden_size)) # h_t+1, zero for the first
            for t in range(T-1, -1, -1):
                outputs_back[:,t,:] = self.bw.forward(x[:,t,:], previous)
                previous = outputs_back[:,t,:].clone()

            # h_t for each t, last h_t of the forward cell, last h_t of the backwards cell
            return torch.cat((outputs, outputs_back), 2), outputs[:,-1,:], outputs_back[:,0,:]
        else:
            # h_t for each t, last h_t of the forward cell
            return outputs, outputs[:,-1,:]



def is_identical(a, b):
    return "Yes" if np.all(np.abs(a - b) < 1e-6) else "No"


if __name__ == '__main__':
    torch.manual_seed(100304343)
    x = torch.randn(5, 3, 10)
    gru = nn.GRU(10, 20, bidirectional=False, batch_first=True)
    outputs, h = gru(x)

    torch.manual_seed(100304343)
    x = torch.randn(5, 3, 10)
    gru2 = GRU2(10, 20, bidirectional=False)
    outputs, h_fw = gru2(x)
    
    print("Checking the unidirectional GRU implementation")
    print("Same hidden states of the forward cell?\t\t{}".format(
        is_identical(h[0].detach().numpy(), h_fw.detach().numpy())
    ))

    torch.manual_seed(100304343)
    x = torch.randn(5, 3, 10)
    gru = GRU2(10, 20, bidirectional=True)
    outputs, h_fw, h_bw = gru(x)

    torch.manual_seed(100304343)
    x = torch.randn(5, 3, 10)
    gru2 = nn.GRU(10, 20, bidirectional=True, batch_first=True)
    outputs, h = gru2(x)
    
    print("Checking the bidirectional GRU implementation")
    print("Same hidden states of the forward cell?\t\t{}".format(
        is_identical(h[0].detach().numpy(), h_fw.detach().numpy())
    ))
    print("Same hidden states of the backward cell?\t{}".format(
        is_identical(h[1].detach().numpy(), h_bw.detach().numpy())
    ))
