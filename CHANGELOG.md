# 1.0.5

* Improved performance by only checking if something can be interacted if its an inventory
* Improved performance by caching the scanned blocks

# 1.0.4

* Changed GUI to make it more compact
* Added support to deposit items into chests, closes #2 closes #7
  * Added a Button to clean inv or hotbar
  * Added settings for inserting and extracting items like:
    * Only insert if there is an item in the chest
    * Leave 1 item in the slot when extracting
* Fixed crashes issues when extracting items from chests closes #1
* Check thread for when testing if the block is interable closes #5 closes #6