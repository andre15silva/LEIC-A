class RemovalStrategy:
    """
    Super class for node removal strategies
    """
    def __init__(self, G):
        self.G = G

    def remove_node(self):
        raise NotImplementedError
