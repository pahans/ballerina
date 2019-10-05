import ballerina/io;
import ballerina/lang.'int as ints;

// The `parse()` function takes a `string` argument and attempts to convert it to an `int`.
function parse(string num) returns int|error {
    return ints:fromString(num);
}

public function main() {
    // The `checkpanic` unary operator can be used to lift errors.
    // In this instance, `checkpanic` is used to lift the (potential) error
    // returned by the `parse()` function. If the actual value returned
    // by the function is an `error`, the function immediately panics
    // with the error.
    // Passing a valid integer as a `string` will return an `int`.
    int y = checkpanic parse("120");
    io:println(y);

    // Passing a random `string` will result in a `panic`.
    int z = checkpanic parse("Invalid");
    io:println(z);
}
