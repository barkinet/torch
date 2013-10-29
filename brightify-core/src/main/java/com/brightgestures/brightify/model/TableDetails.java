package com.brightgestures.brightify.model;

import com.brightgestures.brightify.annotation.Entity;
import com.brightgestures.brightify.annotation.Id;

import java.util.List;

/**
 * @author <a href="mailto:tkriz@redhat.com">Tadeas Kriz</a>
 */
@Entity
public class TableDetails {

    @Id
    public Long id;
    public List<String> columns;

}
