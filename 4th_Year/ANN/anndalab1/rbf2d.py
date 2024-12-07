import numpy as np
import matplotlib.pyplot as plt

class RBFNetwork2D:

    def __init__(self, num_hidden_nodes=13, sigma=0.7, learning_rate=0.05):
        self.hidden_nodes = num_hidden_nodes
        self.w = np.random.normal(0, 1, (num_hidden_nodes, 2))

        self.centers = np.transpose(([np.linspace(0, 1, num_hidden_nodes), np.linspace(0, 1, num_hidden_nodes)]))
        self.sigma = sigma
        self.lr = learning_rate

    def transfer_function(self, data):
        phi = np.zeros((len(data), self.hidden_nodes))
        for idx, x in enumerate(data):
            for j in range(self.hidden_nodes):
                #r = np.linalg.norm(x - self.centers[j])
                r = x - self.centers[j]
                num = -r ** 2
                den = 2 * self.sigma ** 2
                # phi2 = np.exp(-r**2/2*self.sigma**2)
                phi[idx, j] = np.sum(np.exp(num / den))
        return phi

    def train_least_squares(self, data, targets):
        phi = self.transfer_function(data)
        self.w = np.linalg.solve(phi.T @ phi, phi.T @ targets)

    def train_delta_rule(self, data, target):
        #data = data.reshape(1, 2)
        # print(data.shape)
        phi = np.zeros((1, self.hidden_nodes))
        for j in range(self.hidden_nodes):
            #phi[0,j] = np.sum(np.exp(-(data - self.centers[j])**2 / (2 * self.sigma ** 2)))
            c = self.centers[j]
            #c1 = self.centers[j,:]
            #diff = data - c
            r = data - self.centers[j]
            num = -(r**2)
            den = 2*self.sigma**2
            phi1 = np.sum(np.exp(num/den))
            #phi2 = np.exp(-r**2/2*self.sigma**2)
            phi[0,j] = phi1

        #prediction = self.predict_regression([data])[0][0]
        prediction = phi @ self.w
        error = target - prediction
        delta = np.dot(np.transpose(error), phi)
        # print(delta.shape)
        self.w += self.lr * np.transpose(delta)

    def competitive_learning(self, data, k, epochs=1000, avoid_dead_units=True):
        for i in range(epochs):
            point = data[np.random.randint(0, len(data)),:]
            distance = np.zeros((self.hidden_nodes))
            for j in range(self.hidden_nodes):
                distance[j] = np.linalg.norm(self.centers[j] - point)

            winner = np.argmin(distance)
            self.centers[winner] += self.lr * (point - self.centers[winner])
            if avoid_dead_units:
                winners = np.argsort(distance)
                for winner in winners[:k]:
                    self.centers[winner] += self.lr * (point - self.centers[winner])

    def predict_regression(self, data):
        phi = self.transfer_function(data)
        out = phi @ self.w
        return out

    def predict_classify(self, data):
        phi = self.transfer_function(data)
        result = phi @ self.w
        result = [1 if x >= 0 else -1 for x in result]
        return result


def plot_function_prediction(test_targets, predictions, title):
    x = predictions[:, 0]
    y = predictions[:, 1]
    plt.scatter(test_targets[:,0], test_targets[:,1], label="function")
    plt.scatter(predictions[:,0], predictions[:,1], label="prediction")
    plt.legend()
    plt.title(title)
    plt.show()

def mean_absolute_error(predictions, targets):
    mae = np.mean(np.abs(np.subtract(predictions, targets)))
    return mae

def plot_rbf_centers(centers, xlabel, ylabel, title):
    node_number = np.array([i for i in range(1, len(centers)+1)])
    print(centers.shape)
    print(node_number.shape)
    plt.scatter(centers[:,0], centers[:,1])
    plt.axis([-0.5, 1.5, -0.5, 1.5])
    #plt.xlabel(xlabel)
    #plt.ylabel(ylabel)
    plt.title(title)
    plt.show()


## 2D data

train_data = np.loadtxt("ballist.dat")
test_data = np.loadtxt("balltest.dat")
train_patterns = train_data[:,:2]
train_targets = train_data[:,2:4]
test_patterns = test_data[:,:2]
test_targets = test_data[:,2:4]

np.random.seed(0)


#model.train_least_squares(train_patterns, train_targets)
#prediction = model.predict_regression(test_patterns)
#print(prediction.shape)
#print(mean_absolute_error(prediction, test_targets))

#plt.scatter(test_targets[:,0], test_targets[:,1], label="targets")
#plt.scatter(prediction[:,0], prediction[:,1], label="prediction")
#plt.legend()
#plt.show()

nodes = [j for j in range(6, 20)]
sigma = [0.3, 0.5, 0.7, 1]
errors = []
parameters = []
idxs = [j for j in range(len(train_patterns))]

print('without competitive learning')
#for n in nodes:
#    for s in sigma:
model = RBFNetwork2D(13, 0.7, 0.005)
for j in range(100):
    np.random.shuffle(idxs)
    for k in idxs:
        model.train_delta_rule(train_patterns[k,:], train_targets[k,:])
prediction = model.predict_regression(test_patterns)
#prediction = prediction.reshape(1,-1)[0]
plot_function_prediction(test_targets, prediction, "Ballistics data prediction, no CL, 15 hidden nodes, sigma 1")
mae = mean_absolute_error(prediction, test_targets)

#print("num nodes:, ", n, "sigma: ", s, "mae: ", mae)
print("mae ", mean_absolute_error(prediction, test_targets))
#print(model.centers)
plot_rbf_centers(model.centers, "rbf node", "position","Position of RBF centers in input space, no CL")
print("/n")
#best = errors.index(min(errors))
#best_params = parameters[best]
#print("best performer: ", best_params, "error: ", errors[best])


print("with competitive learning")
errors = []
neighbors = [i for i in range(1, 11)]
#for k in neighbors:
model2 = RBFNetwork2D(13, 0.7, 0.005)
model2.competitive_learning(train_patterns, k)

for j in range(100):
    np.random.shuffle(idxs)
    for k in idxs:
        model2.train_delta_rule(train_patterns[k,:], train_targets[k,:])
prediction = model2.predict_regression(test_patterns)
#prediction = prediction.reshape(1,-1)[0]
plot_function_prediction(test_targets, prediction, "Ballistics data prediction, with CL, 15 hidden nodes, sigma 1")
mae = mean_absolute_error(prediction, test_targets)
print(mae)
#errors.append(mae)
#best = errors.index(min(errors))
#k = neighbors[best]
#print("best performer: ", k, "neighbors")
    #print("mae ", mean_absolute_error(prediction, test_targets))

#print(model2.centers)
plot_rbf_centers(model2.centers, "rbf node", "position","rbf centers, competitive learning")
