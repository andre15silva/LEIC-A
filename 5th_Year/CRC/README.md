# CRC Project - André Silva 89408, Bernardo Conde 89418

## Dependencies
Uses `python3`, `networkx`, `matplotlib`, and `numpy`.

## Code structure
The entrypoint for the experimental setup is located at `main.py`.
The plotting is also defined in the same file.
This entrypoint uses all available _\*Strategy.py_ files.
Each _\*Strategy.py_ file implements one of the described strategies in the paper. 
The core experimental logic is located in `Experiment.py`. 
We implemented three possible simulations: a BFS-style simulation (`BFS.py`), SIS simulation (`SIS.py`) and SIR simulation (`SIR.py`), but only use the SIS model.

## Results
All of our paper results are in designated _results_ folders.

## Execution
To run the SIS experiments of the paper:
`python3 main.py`

## Report
The short-paper explaining the work can be found under `report.pdf`
