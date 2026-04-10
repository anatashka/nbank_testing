package middle.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor

public class CreateUserRequest extends BaseModel{
    private String username;
    private String password;
    private String role;


}
