import numpy as np
import matplotlib.pyplot as plt
from Hopfield31rewrite import HopfieldNetwork, testing_loop, find_attractors

model = HopfieldNetwork()

patterns = np.loadtxt("pict.dat", delimiter=",", dtype="int")
patterns = patterns.reshape(11, 1024)
print(patterns.shape)
#print(patterns[0].reshape(32,32))
#plt.imshow(patterns[0].reshape(32,32))
#plt.show()
targets = patterns[0:3,:]
model.learn_weights(targets)
#check for stability
def check_stability(patterns):
    data = patterns[0:3, :]
    targets = patterns[0:3, :]
    print(data.shape)
    model.learn_weights(targets)
    for x in data:
        if model.is_fixed_point(x):
            print("True")
        else:
            print("False")

def plot_image(pattern, title):
    plt.imshow(pattern.reshape(32,32))
    plt.title(title)
    plt.show()

def test_degraded(model, pattern, target, asynch=False, epochs = 10000):
    model.set_pattern(pattern)
    plot_image(model.pattern, "before fixing")
    if np.all(model.pattern == target):
        print("same")
    else:
        print("not same")
    print(model.energy())
    for epoch in range(epochs):
        if epoch % 1000 == 0:
            plot_image(model.pattern, epoch)
            print("epoch: ", epoch)
        if asynch:
            model.update_rule_async()
        else:
            model.update_rule()
        if np.all(model.pattern == target):
            print("pattern fixed after ", epoch, " epochs")
            break
        else:
            continue
    print("failed to converge")
    plot_image(model.pattern, "after fixing")
    print(model.energy())
'''
if np.all(patterns[9] == patterns[0]):
    print("same")
else:
    off = np.sum(np.where(patterns[9] == patterns[0], 0, 1))
    print("off by ", off, " bits")'''

#plot_image(patterns[9], "p10 before fixing")
'''fig, axs = plt.subplots(1,3)
axs[0].imshow(patterns[10].reshape(32,32))
axs[0].set_title("p11")
axs[1].imshow(patterns[1].reshape(32,32))
axs[1].set_title("p2")
axs[2].imshow(patterns[2].reshape(32,32))
axs[2].set_title("p3")
plt.show()'''


#plt.subplot(13)
#test_degraded(model, patterns[9], patterns[0], epochs=10000)
#test_degraded(model, patterns[10], patterns[2], epochs=10000)
test_degraded(model, patterns[10], patterns[2], asynch=True, epochs=10000)
